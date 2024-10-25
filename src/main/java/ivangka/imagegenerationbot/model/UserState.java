package ivangka.imagegenerationbot.model;

public class UserState {

    private long id;
    private String prompt;

    public UserState(long id) {
        this.id = id;
    }

    public void resetFields() {
        prompt = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

}
