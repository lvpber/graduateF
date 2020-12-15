package model.persistmodel;

import lombok.Getter;
import lombok.Setter;
import model.logmodulemodel.LogEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RaftPersistVal implements Serializable {
    private List<LogEntry>  Log;            // 当前节点的日志列表
    private String          VotedFor;       // 当前节点投票的对象
    private Integer         CurrentTerm;    // 当前节点所处任期号

    private RaftPersistVal() {
        Log = new ArrayList<>();
        Log.add(new LogEntry());    // 添加一个空的日志
        CurrentTerm = 0;
        VotedFor = "";
    }
    private static class LazyHolder {
        private static final RaftPersistVal instance = new RaftPersistVal();
    }
    public static RaftPersistVal getInstance() {
        return RaftPersistVal.LazyHolder.instance;
    }

    @Override
    public String toString() {
        String ans =  "{" +
                "VotedFor =" + VotedFor +
                ", CurrentTerm = " + CurrentTerm +
                ", Log = { ";
        for(LogEntry logEntry : Log) {
            ans += logEntry.toString();
        }
        ans += "}";
        return ans;
    }
}
