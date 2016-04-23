package tlb.splitter.correctness;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValidationResultTest {

    @Test
    public void shouldUnderstandSuccess() {
        assertThat(new ValidationResult(ValidationResult.Status.OK).hasFailed(), is(false));
        assertThat(new ValidationResult(ValidationResult.Status.FAILED).hasFailed(), is(true));
        assertThat(new ValidationResult(ValidationResult.Status.FIRST).hasFailed(), is(false));
    }
    
    @Test
    public void shouldUnderstandFailureMessage() {
        ValidationResult result = new ValidationResult(ValidationResult.Status.FAILED, "foo bar");
        assertThat(result.hasFailed(), is(true));
        assertThat(result.getMessage(), is("foo bar"));
    }
}
