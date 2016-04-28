DEV_BOX_TLB_JAR_LOCATION = File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "tlb", "target", "tlb-java*.jar"))
DEV_BOX_TLB_DEPENDENCIES_LOCATION = File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "tlb", "java", "lib", "*.jar"))

DIST_TLB_JAR_LOCATION = File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "java", "tlb-java*.jar"))
DIST_TLB_DEPENDENCIES_LOCATION = File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "java", "lib", "*.jar"))

def tlb_jars
  [Dir[DEV_BOX_TLB_JAR_LOCATION] + Dir[DEV_BOX_TLB_DEPENDENCIES_LOCATION] + Dir[DIST_TLB_JAR_LOCATION] + Dir[DIST_TLB_DEPENDENCIES_LOCATION]].flatten.join(File::PATH_SEPARATOR)
end

if ENV['load_balance'] == 'true'
  Buildr::JUnit.class_eval do
    def run(tests, dependencies) #:nodoc:
      # Use Ant to execute the Junit tasks, gives us performance and reporting.
      Buildr.ant('junit') do |ant|
        case options[:fork]
        when false
          forking = {}
        when :each
          forking = { :fork=>true, :forkmode=>'perTest' }
        when true, :once
          forking = { :fork=>true, :forkmode=>'once' }
        else
          fail 'Option fork must be :once, :each or false.'
        end
        mkpath task.report_to.to_s

        taskdef = Buildr.artifact(JUnit.ant_taskdef)
        taskdef.invoke
        ant.taskdef :name=>'junit', :classname=>'org.apache.tools.ant.taskdefs.optional.junit.JUnitTask', :classpath=>taskdef.to_s
        ant.typedef :name=>'load_balanced_fileset', :classname => 'tlb.ant.LoadBalancedFileSet', :classpath => tlb_jars

        dependencies << tlb_jars

        ant.junit forking.merge(:clonevm=>options[:clonevm] || false, :dir=>task.send(:project).path_to) do
          ant.classpath :path=>dependencies.join(File::PATH_SEPARATOR)
          (options[:properties] || []).each { |key, value| ant.sysproperty :key=>key, :value=>value }
          (options[:environment] || []).each { |key, value| ant.env :key=>key, :value=>value }
          Array(options[:java_args]).each { |value| ant.jvmarg :value=>value }
          ant.formatter :type=>'plain'
          ant.formatter :type=>'plain', :usefile=>false # log test
          ant.formatter :type=>'xml'
          ant.batchtest :todir=>task.report_to.to_s, :failureproperty=>'failed' do
            ant.load_balanced_fileset(:dir => task.compile.target.to_s, :includes => "**/*Test.class")
            ant.formatter :classname=> "tlb.ant.JunitDataRecorder"
            ant.formatter :type=>'plain'
          end
        end
        return tests unless ant.project.getProperty('failed')
      end
      # But Ant doesn't tell us what went kaput, so we'll have to parse the test files.
      tests.inject([]) do |passed, test|
        report_file = File.join(task.report_to.to_s, "TEST-#{test}.txt")
        if File.exist?(report_file)
          report = File.read(report_file)
          # The second line (if exists) is the status line and we scan it for its values.
          status = (report.split("\n")[1] || '').scan(/(run|failures|errors):\s*(\d+)/i).
            inject(Hash.new(0)) { |hash, pair| hash[pair[0].downcase.to_sym] = pair[1].to_i ; hash }
          passed << test if status[:failures] == 0 && status[:errors] == 0
        end
        passed
      end
    end

    namespace 'junit' do
      desc "Generate JUnit tests report in #{report.target}"
      task('report') do |task|
        report.generate Project.projects
        info "Generated JUnit tests report in #{report.target}"
      end
    end
  end
end
