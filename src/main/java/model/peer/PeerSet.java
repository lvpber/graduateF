package model.peer;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PeerSet implements Serializable {
    private List<Peer> list = new ArrayList<>();            /** 记录所有伙伴的消息 Socket */
    private volatile Peer leader;                           /** Leader  */
    private volatile Peer self;                             /** self */


    private PeerSet() {

    }

    /** 静态内部类实现单例模式，多线程安全 */
    public static class SingletonHolder{
        private static PeerSet instance = new PeerSet();
    }

    public static PeerSet getInstance() {
        return SingletonHolder.instance;
    }

    public List<Peer> getPeers(){
        return list;
    }

    public List<Peer> getPeersWithoutSelf(){
        List<Peer> listPeers = new ArrayList<Peer>(list);
        listPeers.remove(self);
        return listPeers;
    }

    @Override
    public String toString() {
        return "PeerSet{" +
                "list=" + list +
                ", leader=" + leader +
                ", self=" + self +
                '}';
    }

    public void addPeer(Peer peer) {
        list.add(peer);
    }

    public void removePeer(Peer peer) {
        list.remove(peer);
    }
}
