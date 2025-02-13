import utils.AccountType;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

abstract class User implements Comparable, Observer {
    private SolvedRequestExperience SolReqExp = new SolvedRequestExperience();
    private ReviewWrittenExperience RevWriExp = new ReviewWrittenExperience();
    private StaffActionExperience StfActExp = new StaffActionExperience();
    private List<Observer> observers = new ArrayList<>();
    private Information information;
    private AccountType type;
    private String username;
    private int exp;
    private List<String> notifications;
    private SortedSet<Actor> favoriteActors;
    private SortedSet<Production> favoriteProductions;
    public static class Information{

        public boolean logIn(Credentials credentials) {
            if (credentials == null) {
                return false;
            }
            return this.getCredentials().equals(credentials);
        }
        // Private inner class
        private static class Credentials {
            private String email;
            private String password;

            private Credentials(Builder builder) {
                this.email = builder.email;
                this.password = builder.password;
            }
            // Getter methods for accessing private fields
            public String getEmail() {
                return email;
            }

            public String getPassword() {
                return password;
            }
            public boolean equals(Credentials credentials){
                return this.email.equals(credentials.email) && this.password.equals(credentials.password);
            }
            public static class Builder {
                private String email;
                private String password;

                public Builder email(String email) {
                    this.email = email;
                    return this;
                }

                public Builder password(String password) {
                    this.password = password;
                    return this;
                }

                public Credentials build() {
                    return new Credentials(this);
                }
            }
        }
        private Credentials credentials;
        private String name, country, gender;
        private int age;
        private LocalDate dateOfBirth;
        private Information(Builder builder){
            this.credentials = builder.credentials;
            this.name = builder.name;
            this.country = builder.country;
            this.age = builder.age;
            this.gender = builder.gender;
            this.dateOfBirth = builder.dateOfBirth;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Credentials.Builder credentialBuilder(){
            return new Credentials.Builder();
        }
        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public int getAge() {
            return age;
        }

        public String getGender() {
            return gender;
        }

        public LocalDate getBirthDate() {
            return dateOfBirth;
        }

        // Getter method for accessing the private Credentials object
        public Credentials getCredentials() {
            return credentials;
        }
        public static class Builder {
            private Credentials credentials;
            private String name;
            private String country;
            private int age;
            private String gender;
            private LocalDate dateOfBirth;

            public Builder credentials(Credentials c) {
                this.credentials = c;
                return this;
            }

            public Builder name(String n) {
                this.name = n;
                return this;
            }

            public Builder country(String c) {
                this.country = c;
                return this;
            }

            public Builder age(int a) {
                this.age = a;
                return this;
            }

            public Builder gender(String g) {
                this.gender = g;
                return this;
            }

            public Builder birthDate(LocalDate bd) {
                this.dateOfBirth = bd;
                return this;
            }

            public Information build() {
                return new Information(this);
            }
        }
    }
    public class ExperienceContext {
        private ExperienceStrategy experienceStrategy;

        // Other fields and methods...

        public ExperienceContext(ExperienceStrategy expStr) {
            this.experienceStrategy = expStr;
        }

        public void setExperienceStrategy(ExperienceStrategy expStr) {
            this.experienceStrategy = expStr;
        }

        public int addExperience() {
            return experienceStrategy.calculateExperience();
        }
    }
    public class SolvedRequestExperience implements ExperienceStrategy {
        private int count = 0;
        public int getCount(){
            return this.count;
        }
        @Override
        public int calculateExperience() {
            int x = 6;
            this.count++;
            if (this.count % 5 == 0) {
                x += 2;
            }
            if (this.count % 10 == 0){
                x += 2;
                this.count = 0;
            }
            return x;
        }
    }

    public class ReviewWrittenExperience implements ExperienceStrategy {
        int count = 0;
        public int getCount(){
            return this.count;
        }
        @Override
        public int calculateExperience() {
            int x = 2;
            this.count++;
            if (this.count % 5 == 0){
                x += 1;
            }
            if (this.count % 10 == 0){
                x += 2;
                this.count = 0;
            }
            return x;
        }
    }

    public class StaffActionExperience implements ExperienceStrategy {
        private int count = 0;
        private int production = 0;
        public void setProduction(){
            this.production = 1;
        }
        @Override
        public int calculateExperience() {
            int x = 4;
            if (production == 1){
                x += 2;
                this.production = 0;
            }
            this.count++;
            if (this.count % 5 == 0){
                this.count = 0;
                x *= 2;
            }
            return x;
        }
    }

    public User(Information information, String type, String username, int experience) {
        this.information = information;
        this.username = username;
        this.exp = experience;
        this.notifications = new ArrayList<>();
        this.favoriteProductions = new TreeSet<>();
        this.favoriteActors = new TreeSet<>();
        try{
            this.type = AccountType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }
    public class userFactory {
        public static User createUser(String type, Information information, String username, int exp) {
            switch (type.toUpperCase()) {
                case "REGULAR": return new Regular(information, username, exp);
                case "CONTRIBUTOR": return new Contributor(information, username, exp);
                case "ADMIN": return new Admin(information, username, exp);
                default: throw new IllegalArgumentException("Unexpected user type: " + type);
            }
        }
    }
    public static String generateUsername(String firstName, String lastName){
        IMDB instance = IMDB.getInstance();
        String str = "";
        boolean uniqueName;
        do {
            uniqueName = true;
            str = firstName + "_" + lastName + "_" + new Random().nextInt(10000);
            for (Regular regular : instance.getRegularList()) {
                if (regular.getUsername().equals(str)) uniqueName = false;
            }
            for (Contributor contributor : instance.getContributorList()) {
                if (contributor.getUsername().equals(str)) uniqueName = false;
            }
            for (Admin admin : instance.getAdminList()) {
                if (admin.getUsername().equals(str)) uniqueName = false;
            }
        }while (!uniqueName);
        return str;
    }
    public static String generatePassword(int length){
        String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
        String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String DIGITS = "0123456789";
        String SPECIAL_CHARACTERS = "!@#$%^&*()-_+=<>?";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++){
            int category = random.nextInt(4);
            switch (category){
                case 0:
                    password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
                    break;
                case 1:
                    password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
                    break;
                case 2:
                    password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
                    break;
                case 3:
                    password.append(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + category);
            }
        }
        return password.toString();
    }
    public void addFavoriteActor(Actor a){
     this.favoriteActors.add(a);
    }
    public void addFavoriteProduction(Production production){
     this.favoriteProductions.add(production);
    }
    public void removeFavoriteActor(Actor a){
     if(!favoriteActors.remove(a)){
      System.out.println("Actor not in list!");
     }
    }
    public void removeFavoriteProduction(Production production){
        if(!favoriteProductions.remove(production)){
            System.out.println("Production not in list!");
        }
    }
    public void setExp(int exp){
        this.exp = exp;
    }
    public int getExp(){
        return this.exp;
    }
    public AccountType getType(){
        return this.type;
    }
    public String getUsername(){
        return this.username;
    }
    public SortedSet<Production> getFavoriteProductions(){
        return this.favoriteProductions;
    }
    public SortedSet<Actor> getFavoriteActors(){
        return this.favoriteActors;
    }
    public List<String> getNotifications(){
        return this.notifications;
    }
    public List<Observer> getObservers(){
        return this.observers;
    }
    public Information getInformation(){
        return this.information;
    }
    @Override
    public void update(String notification) {
        this.notifications.add(notification);
    }
    public void deleteNotification(String notification){
        this.notifications.remove(notification);
    }
    public SolvedRequestExperience getSolReqExp(){
        return this.SolReqExp;
    }
    public ReviewWrittenExperience getRevWriExp(){
        return this.RevWriExp;
    }
    public StaffActionExperience getStfActExp(){
        return this.StfActExp;
    }
    public String toString(){
        String str = "";
        str += "{ " + username + ", " + type + ", " + exp + ", " + information.getName() + ", " + information.getGender() + "\n" + this.favoriteActors.toString() + " }\n";
        return str;
    }
}
