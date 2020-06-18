package innovativedeveloper.com.socialapp.dataset;

public class User {
    private String id;
    private String name, username, email, password, api, registration_id;
    private int relationship, gender, isDisabled, mutualFriends;
    private String location, profilePhoto, profileCover, description, creation;
    private boolean isVerified, isFriend, isFollower, isFollowing;
    public int totalPosts, totalVideos, totalPhotos, totalFriends, totalFollowers;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getApi() {
        return api;
    }

    public String getRegistration_id() {
        return registration_id;
    }

    public int getRelationship() {
        return relationship;
    }

    public int getGender() {
        return gender;
    }

    public int getIsDisabled() {
        return isDisabled;
    }

    public String getLocation() {
        return location;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public String getProfileCover() {
        return profileCover;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public void setRegistration_id(String registration_id) {
        this.registration_id = registration_id;
    }

    public void setRelationship(int relationship) {
        this.relationship = relationship;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setIsDisabled(int isDisabled) {
        this.isDisabled = isDisabled;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public void setProfileCover(String profileCover) {
        this.profileCover = profileCover;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public int getMutualFriends() {
        return mutualFriends;
    }

    public void setMutualFriends(int mutualFriends) {
        this.mutualFriends = mutualFriends;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }

    public boolean isFollower() {
        return isFollower;
    }

    public void setFollower(boolean follower) {
        isFollower = follower;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}
