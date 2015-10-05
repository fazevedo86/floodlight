package pt.ulisboa.tecnico.amorphous.types;

public class NetworkHost extends NetworkNode {

	private static final long serialVersionUID = -1174228150211521562L;

	protected final String MACAddress;
	protected final Short vlan;
	protected final Integer IPAddress;
	
	public NetworkHost(Long nodeId, String MACAddress, Short VLan, Integer IPAddress) {
		super(nodeId, NetworkNodeType.GENERIC_DEVICE);
		this.MACAddress = MACAddress;
		this.vlan = VLan;
		this.IPAddress = IPAddress;
	}

	public String getMACAddress(){
		return this.MACAddress;
	}
	
	public Short getVLan(){
		return this.vlan;
	}
	
	public Integer getIPAddress(){
		return this.IPAddress;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof NetworkHost){
			NetworkHost target = (NetworkHost)obj;
			return this.MACAddress.equals(target.MACAddress) && this.vlan.equals(target.vlan);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return ((this.nodeId.intValue() + this.vlan) / (this.type.ordinal() + 1)) * (this.nodeId.intValue() + this.vlan);
	}
}
