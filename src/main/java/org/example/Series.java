import utils.Genre;

import java.util.*;

public class Series extends Production{
    private int releaseYear, seasonsNumber;
    private Map<String, List<Episode>> seasons = new TreeMap<>();
    public Series(String title, String plot, int releaseYear, int seasonsNumber){
        super(title, plot, (double) 0);
        this.releaseYear = releaseYear;
        this. seasonsNumber = seasonsNumber;
    }
    public Series(String title, String plot, Double averageRating, int releaseYear, int seasonsNumber) {
        super(title, plot, averageRating);
        this.releaseYear = releaseYear;
        this. seasonsNumber = seasonsNumber;
    }
    public void setReleaseYear(int year){
        this.releaseYear = year;
    }
    public void setSeasonsNumber(int number){
        this.seasonsNumber = number;
    }
    public void addSeason(String season, List<Episode> episodes){
        this.seasons.put(season, episodes);
    }
    public int getReleaseYear(){
        return this.releaseYear;
    }
    public int getSeasonsNumber(){
        return this.seasonsNumber;
    }
    public Map<String, List<Episode>> getSeasons() {
        return seasons;
    }
    public String toString(){
        String str = "";
        str += super.toString();
        return str;
    }
    @Override
    public void displayInfo() {
        String formattedTitle;
        if(this.releaseYear > 0) {
            formattedTitle = this.getTitle() + " (" + this.releaseYear + ")";
        }
        else {
            formattedTitle = this.getTitle();
        }
        System.out.println("                     " + formattedTitle);
        System.out.println();
        System.out.print("Genres: ");
        for (Genre genre : this.getGenres()){
            if(Objects.equals(genre, this.getGenres().getLast())) System.out.print(genre.toString() + "\n");
            else System.out.print(genre.toString() + ", ");
        }
        System.out.println("Rating: " + this.getAverageRating());
        System.out.println("Number of seasons: " + this.seasonsNumber);
        System.out.println("Plot: " + this.getPlot());
        System.out.print("Directors: ");
        for (String director : this.getDirectors()){
            if(Objects.equals(director, this.getDirectors().getLast())) System.out.print(director + "\n");
            else System.out.print(director + ", ");
        }
        System.out.print("Actors: ");
        for (String actor : this.getActors()){
            if(Objects.equals(actor, this.getActors().getLast())) System.out.print(actor + "\n");
            else System.out.print(actor + ", ");
        }
        for (Map.Entry<String, List<Episode>> entry : seasons.entrySet()) {
            String seasonNumber = entry.getKey();
            List<Episode> episodes = entry.getValue();

            System.out.println(seasonNumber + ":");
            int i = 0;
            for (Episode episode : episodes) {
                i++;
                System.out.println("  Episode " + i + " - " + episode.getEpisodeName());
            }
        }
    }
}
