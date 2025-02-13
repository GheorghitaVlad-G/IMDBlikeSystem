import java.util.ArrayList;
import java.util.List;

public class Admin extends Staff{

    @Override
    public void notifyObservers(String notification) {
        for (Observer observer : this.getObservers()){
            observer.update(notification);
        }
    }

    public static class RequestsHolder {
        private static List<Request> teamRequests = new ArrayList<>();

        private RequestsHolder() {
        }
        public static List<Request> getTeamRequests() {
            return teamRequests;
        }
    }
    public Admin(Information info, String un, int exp) {
        super(info, "Admin", un, exp);
    }

    public String toString(){
        String str = "";
        str += "{\n" + this.getUsername() + "\n" + this.getActorsAdded() + "\n" + getProductionsAdded() + "\n}";
        return str;
    }

    @Override
    public int compareTo(Object o) {
        Admin admin = (Admin) o;
        return admin.getUsername().compareTo(this.getUsername());
    }
    private void deleteUserFromAllLists(String name){
        IMDB instance = IMDB.getInstance();
        // remove Requests
        for (Contributor contributor : instance.getContributorList()){
            contributor.getOwnList().removeIf(request -> request.getCreatorUsername().equals(name));
        }
        for (Admin admin : instance.getAdminList()){
            admin.getOwnList().removeIf(request -> request.getCreatorUsername().equals(name));
        }
        Admin.RequestsHolder.getTeamRequests().removeIf(request -> request.getCreatorUsername().equals(name));

        User del = null;
        for(Regular regular : instance.getRegularList()){
            if (regular.getUsername().equals(name)) {
                del = regular;
                break;
            }
        }
        for(Contributor contributor : instance.getContributorList()){
            if (contributor.getUsername().equals(name)) {
                del = contributor;
                break;
            }
        }
        // remove ratings
        if(del != null) {
            for (Production production : instance.getMovies()) {
                if (del instanceof Regular) production.getHasBeenReviewedBy().remove(del);
                production.getRatings().removeIf(rating -> rating.getUser().equals(name));
                production.calculateAverageRating();
            }
            for (Production production : instance.getSeries()) {
                if (del instanceof Regular) production.getHasBeenReviewedBy().remove(del);
                production.getRatings().removeIf(rating -> rating.getUser().equals(name));
                production.calculateAverageRating();
            }
        }
    }
    public boolean removeUser(String name){
        IMDB instance = IMDB.getInstance();
        boolean wasDeleted;
        deleteUserFromAllLists(name);
        wasDeleted = instance.getRegularList().removeIf(regular -> regular.getUsername().equals(name));
        if (wasDeleted) {
            return true;
        }
        wasDeleted = instance.getContributorList().removeIf(contributor -> contributor.getUsername().equals(name));
        if (wasDeleted) {
            return true;
        }
        wasDeleted = instance.getAdminList().removeIf(admin -> admin.getUsername().equals(name));
        return wasDeleted;
    }
}
