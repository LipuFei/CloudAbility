/**
 * Modified to be a general utility module.
 * by Lipu Fei, 2012
 */
package org.koala.internals;

import java.io.File;
import java.io.IOException;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.Session;

import org.apache.log4j.Logger;

/**
 * Utility class for SSH.
 * @author KOALA
 * @version 2.1
 *
 */
public class SSHHandler {

	private static Logger logger = Logger.getLogger(SSHHandler.class);

	private static final String known_hosts = System.getenv("HOME")+"/.ssh/known_hosts";
	private static final String keyfile_dsa = System.getenv("HOME") + "/.ssh/id_dsa";
	private static final String keyfile_rsa = System.getenv("HOME") + "/.ssh/id_rsa";
	private static KnownHosts database = new KnownHosts();

	private  String getKeyFile(){
		if(new File(keyfile_dsa).exists()){
			return keyfile_dsa;
		}
		if(new File(keyfile_rsa).exists()){
			return keyfile_rsa;
		}
		return null;
	}

	private Connection getConnection(String hostname) throws SSHException {
		String priv_key = this.getKeyFile();
		if (priv_key == null){
			logger.error("DM - Keyfile id_rsa or id_dsa not present in the user ~/.ssh/ directory");
			throw new RuntimeException();
		}
		Connection conn = null;
		try {
			File keyfile = new File(priv_key);
			String keyfilePass = ""; 
			File knownHosts = new File(known_hosts);
			if (knownHosts.exists()) {
				database.addHostkeys(knownHosts);
			}

			conn = new Connection(hostname);
			conn.connect(new SimpleVerifier(database));
			boolean isAuthenticated = conn.authenticateWithPublicKey(System.getenv("USER"), keyfile, keyfilePass);
		
			if (isAuthenticated == false) {
				logger.error("Authentication with the ssh public key failed");
				throw new RuntimeException();
			}
		} catch(IOException e){
			if(conn != null){
				conn.close();
			}
			logger.error("SSH connection to the remote host has failed");
			throw new SSHException(e);
		}
		return conn;
	}

	public static synchronized Session getSession(String hostname) throws SSHException {
		SSHHandler ssh = new SSHHandler();
		Session sess = null;
		try {
			Connection conn = ssh.getConnection(hostname);
			sess =  conn.openSession();
		} catch(IOException e) {
			logger.error("Failed to open SSH session with the remote host " + hostname);
			throw new SSHException(e);
		}
		return sess;
	}

	public static synchronized SFTPv3Client getSftpClient(String hostname) throws SSHException {
		SSHHandler ssh = new SSHHandler();
		SFTPv3Client sftp = null;
		try {
			Connection conn = ssh.getConnection(hostname);
			sftp = new SFTPv3Client(conn);
		} catch(IOException e) {
			logger.error("Failed to open SFTP connection to the remote host " + hostname);
			throw new SSHException(e);
		}
		return sftp;
	}

	private class SimpleVerifier implements ServerHostKeyVerifier{
		KnownHosts database;

		public SimpleVerifier(KnownHosts database){
			if (database == null){
				throw new IllegalArgumentException();
			}
			this.database = database;
		}

		public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
				throws IllegalStateException, IOException{
			int result = database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
			switch (result){
			case KnownHosts.HOSTKEY_IS_OK:
				return true;
			case KnownHosts.HOSTKEY_IS_NEW:
				database.addHostkey(new String[] { hostname }, serverHostKeyAlgorithm, serverHostKey);
				return true;
			case KnownHosts.HOSTKEY_HAS_CHANGED:
				System.err.println("SSH - Hostkey has changed; probably an intrusion of some sort. \n" +
						"Delete the hostname entry in the knownhost file and retry ");
				return false;
			default:
				throw new IllegalStateException();
			}
		}
	}

}
