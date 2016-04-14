package delegator;

import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

public class CustomUser implements User {

	@Override
	public AuthorizationRequest authorize(AuthorizationRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Authority> getAuthorities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Authority> getAuthorities(Class<? extends Authority> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getHomeDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxIdleTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return null;
	}

}
