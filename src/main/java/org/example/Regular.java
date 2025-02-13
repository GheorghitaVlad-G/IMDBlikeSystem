import utils.RequestTypes;
import java.util.List;

public class Regular extends User implements RequestsManager, Subject{
    public Regular(Information info, String un, int exp) {
        super(info, "Regular", un, exp);
    }
    @Override
    public void createRequest (Request r){
        IMDB instance = IMDB.getInstance();
        String notification;

        if (r.getRequestType().equals(RequestTypes.MOVIE_ISSUE)){
            Staff staff = findStaffMember(instance.getAdminList(), r.getTitleOrMovie());
            if (staff == null) {
                staff = findStaffMember(instance.getContributorList(), r.getTitleOrMovie());
            }
            staff.getOwnList().add(r);
            this.getObservers().add(staff);
            notification = "You recieved a request from " + this.getUsername() + ".\n";
        }
        else if (r.getRequestType().equals(RequestTypes.ACTOR_ISSUE)){
            Staff staff = findStaffMemberActor(instance.getAdminList(), r.getTitleOrMovie());
            if (staff == null) {
                staff = findStaffMemberActor(instance.getContributorList(), r.getTitleOrMovie());
            }
            staff.getOwnList().add(r);
            this.getObservers().add(staff);
            notification = "You recieved a request from " + this.getUsername() + ".\n";
        }
        else{
            Admin.RequestsHolder.getTeamRequests().add(r);
            this.getObservers().addAll(instance.getAdminList());
            notification = "The administration team recieved a request from " + this.getUsername() + ".\n";
        }
        // send notifications
        notifyObservers(notification);
        this.getObservers().clear();
    }
    private Staff findStaffMember(List<? extends Staff> staffList, String name) {
        return staffList.stream()
                .filter(user -> user.getProductionsAdded().stream().anyMatch(production -> production.getTitle().equals(name)))
                .findFirst()
                .orElse(null);
    }
    private Staff findStaffMemberActor(List<? extends Staff> staffList, String name) {
        return staffList.stream()
                .filter(user -> user.getActorsAdded().stream().anyMatch(actor -> actor.getActorName().equals(name)))
                .findFirst()
                .orElse(null);
    }
    public void removeRequest(Request r) {
        IMDB instance = IMDB.getInstance();
        if (r.getRequestType().equals(RequestTypes.ACTOR_ISSUE) || r.getRequestType().equals(RequestTypes.MOVIE_ISSUE)) {
            instance.getAdminList().forEach(admin -> admin.getOwnList().removeIf(request -> request.equals(r)));
            instance.getContributorList().forEach(contributor -> contributor.getOwnList().removeIf(request -> request.equals(r)));
        }
        else {
            Admin.RequestsHolder.getTeamRequests().remove(r);
        }
    }

    public void addRating(String comment, int rating, Production production){
        IMDB instance = IMDB.getInstance();
        String notification;
        // add exp
        if (!production.getHasBeenReviewedBy().get(this)) {
            ExperienceContext context = new ExperienceContext(this.getRevWriExp());
            this.setExp(this.getExp() + context.addExperience());
            production.getHasBeenReviewedBy().put(this, true);
        }
        // find the staff member that added the production
        Staff staff = findStaffMember(instance.getContributorList(), production.getTitle());
        if (staff == null) {
            staff = findStaffMember(instance.getAdminList(), production.getTitle());
        }
        // send notifications
        this.getObservers().add(staff);
        notification = "The production '" + production.getTitle() + "' you added in the system received a rating.";
        if (!staff.getFavoriteProductions().contains(production)) notifyObservers(notification);
        this.getObservers().remove(staff);
        if(!production.getRatings().isEmpty()) {
            notification = "The production '" + production.getTitle() + "' you reviewed received a rating";
            for (Rating review : production.getRatings()){
                for (User user : instance.getRegularList()){
                    if (user.getUsername().equals(review.getUser())){
                        this.getObservers().add(user);
                        break;
                    }
                }
            }
            notifyObservers(notification);
            this.getObservers().clear();
        }
        notification = "The production '" + production.getTitle() + "' you favorited received a rating";
        for (Regular regular : instance.getRegularList()){
            if (regular.getFavoriteProductions().contains(production)){
                this.getObservers().add(regular);
            }
        }
        for (Contributor contributor : instance.getContributorList()){
            if (contributor.getFavoriteProductions().contains(production)){
                this.getObservers().add(contributor);
            }
        }
        for (Admin admin : instance.getAdminList()){
            if (admin.getFavoriteProductions().contains(production)){
                this.getObservers().add(admin);
            }
        }
        notifyObservers(notification);
        this.getObservers().clear();
        // add rating to production
        Rating newRating = new Rating(this.getUsername(), comment, rating);
        production.addRating(newRating);
        for (Production production1 : IMDB.getInstance().getMovies()){
            production1.sortRatings();
        }
        for (Production production1 : IMDB.getInstance().getSeries()){
            production1.sortRatings();
        }
    }
    @Override
    public int compareTo(Object o) {
        Regular regular = (Regular) o;
        return this.getExp() - regular.getExp();
    }
    @Override
    public void notifyObservers(String notification) {
        for (Observer observer : this.getObservers()){
            observer.update(notification);
        }
    }
    public String toString(){
        return "\n" + super.getUsername() + " " + super.getExp();
    }
}
