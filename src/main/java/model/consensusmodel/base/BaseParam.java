package model.consensusmodel.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseParam implements Serializable {
    private int term;				/** 发送者任期号 */
    private String serverId;		/** 被请求者id (IP:Port) */

    @Override
    public String toString() {
        return "BaseParam{" +
                "term=" + getTerm() +
                ", serverId='" + getServerId() + '\'' +
                '}';
    }
}
