package model.peer;

import java.io.Serializable;
import java.util.Objects;

public class Peer implements Serializable {
    private final String addr;

    public Peer() {addr = "";}

    public Peer(String addr) { this.addr = addr;}

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        if( obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Peer peer = (Peer) obj;
        return Objects.equals(addr, peer.getAddr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr);
    }

    @Override
    public String toString() {
        return "Peer { '" +
                addr + "\'" +
                " }";
    }

    public String getAddr() {
        return addr;
    }
}
