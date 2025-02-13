import utils.RequestTypes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Request {
    private RequestTypes requestType;
    private LocalDateTime time;
    private String nameOrMovie;
    private String description;
    private String creatorUsername;
    private String solverUsername;
    public Request(){
        this.requestType = RequestTypes.OTHERS;
        this.time = LocalDateTime.now();
        this.nameOrMovie = "";
        this.description = "";
        this.creatorUsername = "";
        this.solverUsername = "";
    }
    public Request(String type, String time, String descr, String creator, String solver){
        this.requestType = RequestTypes.valueOf(type.toUpperCase());
        this.description = descr;
        this.creatorUsername = creator;
        this.solverUsername = solver;
        this.nameOrMovie = "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        try{
            this.time = LocalDateTime.parse(time, formatter);
        }
        catch (DateTimeParseException e){
            System.err.println("Error parsing the string: " + e.getMessage());
        }
    }
    public Request(String type, String time, String descr, String creator, String solver, String title){
        this(type, time, descr, creator, solver);
        this.nameOrMovie = title;
    }
    public RequestTypes getRequestType(){
        return this.requestType;
    }
    public LocalDateTime getTime(){
        return this.time;
    }
    public String getTitleOrMovie(){
        return this.nameOrMovie;
    }
    public String getDescription(){
        return this.description;
    }
    public String getCreatorUsername(){
        return this.creatorUsername;
    }
    public String getSolverUsername(){
        return this.solverUsername;
    }
    public void setRequestType(RequestTypes requestType) {
        this.requestType = requestType;
    }
    public void setTime(LocalDateTime time) {
        this.time = time;
    }
    public void setNameOrMovie(String nameOrMovie) {
        this.nameOrMovie = nameOrMovie;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
    public void setSolverUsername(String solverUsername) {
        this.solverUsername = solverUsername;
    }
    public String toString(){
        String str = "";
        str += "\n{\n" + this.getRequestType().toString() + "\n" + getCreatorUsername() + "\n" + getSolverUsername() + "\n" + getTime().toString() + "\n" + getDescription() + "\n";
        if(!getTitleOrMovie().isEmpty()){
            str += getTitleOrMovie() + "\n";
        }
        str += "}";
        return str;
    }
}
