import utils.RequestTypes;

import java.time.LocalDateTime;
import java.util.List;

public class Contributor extends Staff implements RequestsManager{
    public Contributor(Information info, String un, int exp) {
        super(info,"Contributor", un, exp);
    }
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
    private boolean handleRequestForStaff(Request r, List<? extends Staff> staffList, Production production) {
        return staffList.stream()
                .filter(user -> user.getProductionsAdded().contains(production))
                .findFirst()
                .map(staff -> {
                    r.setSolverUsername(staff.getUsername());
                    staff.getOwnList().add(r);
                    this.getObservers().add(staff);
                    return true;
                })
                .orElse(false);
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
    public String toString(){
        String str = "";
        //str += "{\n" + this.getUsername() + "\n" + this.getActorsAdded() + "\n" + getProductionsAdded() + "\n}";
        str = super.getUsername() + "\n" + super.getExp() + "\n";
        return str;
    }

    @Override
    public int compareTo(Object o) {
        Contributor contributor = (Contributor) o;
        return this.getExp() - contributor.getExp();
    }
    @Override
    public void notifyObservers(String notification) {
        for (Observer observer : this.getObservers()){
            observer.update(notification);
        }
    }
}
