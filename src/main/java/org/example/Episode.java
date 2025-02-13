public class Episode {
    private String episodeName;
    private String episodeLength;
    public Episode(String name, String episodeLength){
        this.episodeLength = episodeLength;
        this.episodeName = name;
    }
    public void setEpisodeName(String s){
        this.episodeName = s;
    }
    public void setEpisodeLength(String l){
        this.episodeLength = l;
    }
    public String getEpisodeName(){
        return this.episodeName;
    }
    public String getEpisodeLength(){
        return this.episodeLength;
    }
}
