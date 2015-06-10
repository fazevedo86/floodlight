package pt.ulisboa.tecnico.amorphous.cluster;

public class ClusterService implements IAmorphousCluster {

	protected ClusterListner listner;
	protected ClusterCommunicator sender;
	
	public ClusterService(String MulticastGroup, String Port) {
		
	}

	@Override
	public boolean startCluster() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopCluster() {
		// TODO Auto-generated method stub
		return false;
	}

}
