package model.node;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NodeConfig {
    public int selfPort;
    public String selfIpAddr;
    public List<String> peerAddrs;
}
