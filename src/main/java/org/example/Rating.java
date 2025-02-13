public class Rating {
    private String user, comment;
    private int rating;
    public Rating(String user, String comment, int rating){
        this.user = user;
        this.comment = comment;
        rating = rating % 11;
        if(rating == 0) rating = 1;
        this.rating = rating;
    }
    public String getUser(){
        return this.user;
    }
    public String getComment(){
        return this.comment;
    }
    public int getRating(){
        return this.rating;
    }
    public String toString(){
        String str = "";
        str += "\n{ " + this.user + " " + this.rating + " }\n";
        return str;
    }
}
