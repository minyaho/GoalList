package yuntech.goalteam.goallist.List;

public class ListItem {
    private int id;
    private String title;
    private String context;
    private String remain;
    private boolean done;
    private boolean notify;
    private long start_time;
    private long end_time;
    private long notification_time;


    public ListItem(int id, String title, String context, String remain, boolean done, boolean notify, long start_time, long end_time, long notification_time) {
        this.id = id;
        this.title = title;
        this.context = context;
        this.remain = remain;
        this.done = done;
        this.notify = notify;
        this.start_time = start_time;
        this.end_time = end_time;
        this.notification_time = notification_time;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setRemain(String remain) {
        this.remain = remain;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    public void setNotification_time(long notification_time) {
        this.notification_time = notification_time;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContext() {
        return context;
    }

    public String getRemain() {
        return remain;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isNotify() {
        return notify;
    }

    public long getStart_time() {
        return start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public long getNotification_time() {
        return notification_time;
    }
}
