package SyncMain;

public abstract class StatusLogger {
	public abstract void show(String srvName, String msg);
	public abstract void addService(String srvName);
	public abstract void updateSpace(String srvName, long used, long total);
}
