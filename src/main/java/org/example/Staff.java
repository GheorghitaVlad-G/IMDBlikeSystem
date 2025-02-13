import utils.RequestTypes;

import java.util.*;

public abstract class Staff extends User implements StaffInterface, Subject{
    private List<Request> ownList = new ArrayList<>();
    private SortedSet<Actor> actorsAdded = new TreeSet<>();
    private SortedSet<Production> productionsAdded = new TreeSet<>();
    public Staff(Information info,String type, String un, int exp) {
        super(info, type, un, exp);
    }
    public void addActors(Actor a){
        this.actorsAdded.add(a);
    }
    public void removeActors(Actor a){
        this.actorsAdded.remove(a);
    }
    public void addProduction(Production p){
        this.productionsAdded.add(p);
    }
    public void removeProduction(Production p){
        this.productionsAdded.remove(p);
    }
    public SortedSet<Actor> getActorsAdded(){
        return this.actorsAdded;
    }
    public SortedSet<Production> getProductionsAdded(){
        return this.productionsAdded;
    }
    public List<Request> getOwnList(){
        return this.ownList;
    }
    @Override
    public void addProductionSystem(Production p) {
        IMDB instance = IMDB.getInstance();
        // add all users to the review map
        for (Regular regular : instance.getRegularList()){
            p.addRegularsToReviews(regular);
        }
        //add production to staff member's contribution list
        this.addProduction(p);
        //add production to movie list
        if (p instanceof Movie){
            instance.getMovies().add((Movie) p);
        }
        else{
            instance.getSeries().add((Series) p);
        }
        // add exp
        if (this instanceof Contributor){
            this.getStfActExp().setProduction();
            ExperienceContext context = new ExperienceContext(this.getStfActExp());
            this.setExp(this.getExp() + context.addExperience());
        }
    }

    @Override
    public void addActorSystem(Actor a) {
        IMDB instance = IMDB.getInstance();
        instance.getActors().add(a);
        this.addActors(a);
        // add exp
        if (this instanceof Contributor){
            ExperienceContext context = new ExperienceContext(this.getStfActExp());
            this.setExp(this.getExp() + context.addExperience());
        }
    }
    private <T> void removeFavoriteActorsFromUsers(List<T> list, String name){
        for(Object o : list){
            User user = (User) o;
            for (Actor actor : user.getFavoriteActors()){
                if (actor.getActorName().equals(name)){
                    user.getFavoriteActors().remove(actor);
                    break;
                }
            }
        }
    }
    @Override
    public void removeActorSystem(String name) {
        IMDB instance = IMDB.getInstance();
        removeFavoriteActorsFromUsers(instance.getRegularList(), name);
        removeFavoriteActorsFromUsers(instance.getContributorList(), name);
        removeFavoriteActorsFromUsers(instance.getAdminList(), name);

        for (Actor a : instance.getActors()){
            if (name.equals(a.getActorName())){
                instance.getActors().remove(a);
                this.getActorsAdded().remove(a);
                break;
            }
        }
    }
    private <T> void removeFavoriteProductionsFromUsers(List<T> list, String name){
        for(Object o : list){
            User user = (User) o;
            for (Production production : user.getFavoriteProductions()){
                if (production.getTitle().equals(name)){
                    user.getFavoriteProductions().remove(production);
                    break;
                }
            }
        }
    }
    private <T> void addFavoriteProductionsToUsers(List<T> list, Production production){
        for(Object o : list){
            User user = (User) o;
            user.getFavoriteProductions().add(production);
        }
    }
    @Override
    public void removeProductionSystem(String name) {
        IMDB instance = IMDB.getInstance();

        removeFavoriteProductionsFromUsers(instance.getRegularList(), name);
        removeFavoriteProductionsFromUsers(instance.getContributorList(), name);
        removeFavoriteProductionsFromUsers(instance.getAdminList(), name);

        if(!instance.getMovies().removeIf(movie -> name.equals(movie.getTitle()))){
            instance.getSeries().removeIf(series -> name.equals(series.getTitle()));
        }
        for (Production production : this.getProductionsAdded()){
            if (production.getTitle().equals(name)){
                this.getProductionsAdded().remove(production);
                break;
            }
        }
    }

    private static void copyRatings(Production p, Production production){
        for (Rating rating : production.getRatings()){
            p.addRating(rating);
        }
        for (Map.Entry<Regular, Boolean> entry : production.getHasBeenReviewedBy().entrySet()){
            p.getHasBeenReviewedBy().put(entry.getKey(), entry.getValue());
        }
    }
    @Override
    public void updateProduction(Production p) {
        IMDB instance = IMDB.getInstance();
        boolean added = false;
        for (Series series : instance.getSeries()){
            if (series.getTitle().equals(p.getTitle())){
                instance.getSeries().remove(series);
                instance.getSeries().add((Series) p);

                removeFavoriteProductionsFromUsers(instance.getRegularList(), series.getTitle());
                removeFavoriteProductionsFromUsers(instance.getContributorList(), series.getTitle());
                removeFavoriteProductionsFromUsers(instance.getAdminList(), series.getTitle());

                addFavoriteProductionsToUsers(instance.getAdminList(), p);
                addFavoriteProductionsToUsers(instance.getContributorList(), p);
                addFavoriteProductionsToUsers(instance.getRegularList(), p);

                copyRatings(p, series);

                this.getProductionsAdded().remove(series);
                this.getProductionsAdded().add(p);
                added = true;
                break;
            }
        }
        if(!added) {
            for (Movie movie : instance.getMovies()) {
                if (movie.getTitle().equals(p.getTitle())) {
                    instance.getMovies().remove(movie);
                    instance.getMovies().add((Movie) p);

                    removeFavoriteProductionsFromUsers(instance.getRegularList(), movie.getTitle());
                    removeFavoriteProductionsFromUsers(instance.getContributorList(), movie.getTitle());
                    removeFavoriteProductionsFromUsers(instance.getAdminList(), movie.getTitle());

                    addFavoriteProductionsToUsers(instance.getAdminList(), p);
                    addFavoriteProductionsToUsers(instance.getContributorList(), p);
                    addFavoriteProductionsToUsers(instance.getRegularList(), p);

                    copyRatings(p, movie);

                    this.getProductionsAdded().remove(movie);
                    this.getProductionsAdded().add(p);
                    break;
                }
            }
        }
    }

    @Override
    public void updateActor(Actor a) {
        //faci cu citire
        IMDB instance = IMDB.getInstance();
        for (Actor actor : instance.getActors()){
            if (actor.getActorName().equals(a.getActorName())){
                instance.getActors().remove(actor);
                instance.getActors().add(a);

                instance.getRegularList().getFirst().getFavoriteActors().remove(actor);
                instance.getRegularList().getFirst().getFavoriteActors().add(a);

                instance.getContributorList().getFirst().getFavoriteActors().remove(actor);
                instance.getContributorList().getFirst().getFavoriteActors().add(a);

                instance.getAdminList().getFirst().getFavoriteActors().remove(actor);
                instance.getAdminList().getFirst().getFavoriteActors().add(a);


                this.getActorsAdded().remove(actor);
                this.getActorsAdded().add(a);
                break;
            }
        }
    }
    public void solveRequest(Request r, String solvedOrRejected){
        IMDB instance = IMDB.getInstance();
        String notification;
        // get user that made the request
        User solvedUser = instance.getRegularList().stream()
                .filter(regular -> regular.getUsername().equals(r.getCreatorUsername()))
                .findFirst()
                .orElse(null);
        if (solvedUser == null){
            solvedUser = instance.getContributorList().stream()
                    .filter(regular -> regular.getUsername().equals(r.getCreatorUsername()))
                    .findFirst()
                    .orElse(null);
        }
        //add exp if request solved
        if(solvedOrRejected.equals("SOLVED") && (r.getRequestType().equals(RequestTypes.ACTOR_ISSUE) || r.getRequestType().equals(RequestTypes.MOVIE_ISSUE))) {
            ExperienceContext context = new ExperienceContext(solvedUser.getSolReqExp());
            solvedUser.setExp(solvedUser.getExp() + context.addExperience());
        }
        // notify
        this.getObservers().add(solvedUser);
        notification = "Your request addressed to " + r.getSolverUsername() + " has been " + solvedOrRejected +".\n";
        this.notifyObservers(notification);
        this.getObservers().clear();
        //remove request from request lists
        if (r.getSolverUsername().equals("ADMIN")){
            Admin.RequestsHolder.getTeamRequests().remove(r);
        }
        else {
            this.ownList.remove(r);
        }
    }
}
