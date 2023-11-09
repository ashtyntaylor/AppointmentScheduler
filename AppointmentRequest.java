import java.util.List;

public class AppointmentRequest {
    private int requestId = -1;
    private int personId = -1;
    private List<String> preferredDays = null;
    private List<Integer> preferredDocs = null;
    private boolean isNew = false;

    public AppointmentRequest(int requestId, int personId, List<String> preferredDays, List<Integer> preferredDocs, boolean isNew) {
        this.requestId = requestId;
        this.personId = personId;
        this.preferredDays = preferredDays;
        this.preferredDocs = preferredDocs;
        this.isNew = isNew;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public List<String> getPreferredDays() {
        return preferredDays;
    }

    public void setPreferredDays(List<String> preferredDays) {
        this.preferredDays = preferredDays;
    }

    public List<Integer> getPreferredDocs() {
        return preferredDocs;
    }

    public void setPreferredDocs(List<Integer> preferredDocs) {
        this.preferredDocs = preferredDocs;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }
}
