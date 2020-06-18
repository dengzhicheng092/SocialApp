package innovativedeveloper.com.socialapp.dataset;
public class Like {

    private User user;
    private String postId;
    private String creation;
    private String icon;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCreation() {
        return creation;
    }

    public String getPostid() {
        return postId;
    }

    public String getIcon() {
        return icon;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public void setPostid(String postId) {
        this.postId = postId;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
