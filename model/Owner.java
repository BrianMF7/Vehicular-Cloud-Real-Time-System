package model;

public class Owner {

    /* Private field uses encapsulation so other classes cant directly access or change the owner's ID */
    private String ownerId;

    public Owner(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return "Owner ID: " + ownerId;
    }
}
