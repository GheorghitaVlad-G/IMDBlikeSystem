import org.json.JSONArray;
import org.json.JSONObject;
import utils.AccountType;
import utils.Genre;
import utils.RequestTypes;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class IMDB {
    public static Series auxiliarySeries;
    public static Movie auxiliaryMovie;
    public static Actor auxiliaryActor;
    private static IMDB instance;
    private static List<Regular> regularList = new ArrayList<>();
    private static List<Contributor> contributorList = new ArrayList<>();
    private static List<Admin> adminList = new ArrayList<>();
    private static List<Actor> actors = new ArrayList<>();
    private static List<Request> requests = new ArrayList<>();
    private static List<Movie> movies = new ArrayList<>();
    private static List<Series> series = new ArrayList<>();

    public List<Movie> getMovies() {
        return movies;
    }

    public List<Regular> getRegularList() {
        return regularList;
    }

    public List<Contributor> getContributorList() {
        return contributorList;
    }

    public List<Admin> getAdminList() {
        return adminList;
    }

    public List<Series> getSeries() {
        return series;
    }

    public List<Actor> getActors() {
        return actors;
    }

    private IMDB() {

    }

    public static IMDB getInstance() {
        if (instance == null) {
            return new IMDB();
        }
        return instance;
    }

    private static Staff addContributions(User user, JSONObject jsonObj) {
        Staff contributor = (Staff) user;
        if (!jsonObj.isNull("productionsContribution")) {
            JSONArray productionsContribution = jsonObj.getJSONArray("productionsContribution");
            for (int j = 0; j < productionsContribution.length(); j++) {
                String production = productionsContribution.getString(j);
                boolean ok = false;
                for (Movie prod : movies) {
                    if (prod.getTitle().equals(production)) {
                        contributor.addProduction(prod);
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    for (Series prod : series) {
                        if (prod.getTitle().equals(production)) {
                            contributor.addProduction(prod);
                            break;
                        }
                    }
                }
            }
        }
        if (!jsonObj.isNull("actorsContribution")) {
            JSONArray actorsContribution = jsonObj.getJSONArray("actorsContribution");
            for (int j = 0; j < actorsContribution.length(); j++) {
                String actor = actorsContribution.getString(j);
                for (Actor act : actors) {
                    if (act.getActorName().equals(actor)) {
                        contributor.addActors(act);
                        break;
                    }
                }
            }
        }
        return contributor;
    }

    private static void readUsers(String path) {
        try {
            FileReader reader = new FileReader(path);
            char[] buffer = new char[1024];
            int bytesRead;
            StringBuilder content = new StringBuilder();
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(content.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                String username = jsonObj.getString("username");
                int exp;
                if (!jsonObj.isNull("experience")) {
                    exp = Integer.parseInt(jsonObj.getString("experience"));
                } else {
                    exp = 999;
                }

                JSONObject jsonInfo = jsonObj.getJSONObject("information");
                // Credentials
                JSONObject jsonCredentials = jsonInfo.getJSONObject("credentials");
                String email = jsonCredentials.getString("email");
                String password = jsonCredentials.getString("password");
                // Rest of information
                String name = jsonInfo.getString("name");
                String country = jsonInfo.getString("country");
                int age = jsonInfo.getInt("age");
                String gender = jsonInfo.getString("gender");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate dateOfBirth = LocalDate.parse(jsonInfo.getString("birthDate"), formatter);

                User.Information information = User.Information.builder()
                        .credentials(User.Information.credentialBuilder()
                                .email(email)
                                .password(password)
                                .build())
                        .name(name)
                        .country(country)
                        .age(age)
                        .gender(gender)
                        .birthDate(dateOfBirth)
                        .build();
                String type = jsonObj.getString("userType");
                try {
                    User user = User.userFactory.createUser(type, information, username, exp);
                    if (!jsonObj.isNull("favoriteProductions")) {
                        JSONArray favoriteProductions = jsonObj.getJSONArray("favoriteProductions");
                        for (int j = 0; j < favoriteProductions.length(); j++) {
                            String production = favoriteProductions.getString(j);
                            boolean ok = false;
                            for (Movie prod : movies) {
                                if (prod.getTitle().equals(production)) {
                                    user.addFavoriteProduction(prod);
                                    ok = true;
                                    break;
                                }
                            }
                            if (!ok) {
                                for (Series prod : series) {
                                    if (prod.getTitle().equals(production)) {
                                        user.addFavoriteProduction(prod);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!jsonObj.isNull("favoriteActors")) {
                        JSONArray favoriteProductions = jsonObj.getJSONArray("favoriteActors");
                        for (int j = 0; j < favoriteProductions.length(); j++) {
                            String actor = favoriteProductions.getString(j);
                            for (Actor act : actors) {
                                if (act.getActorName().equals(actor)) {
                                    user.addFavoriteActor(act);
                                    break;
                                }
                            }
                        }
                    }

                    switch (user.getType().toString().toUpperCase()) {
                        case "REGULAR":
                            for (Production production : movies) {
                                production.addRegularsToReviews((Regular) user);
                            }
                            for (Production production : series) {
                                production.addRegularsToReviews((Regular) user);
                            }
                            regularList.add((Regular) user);
                            break;
                        case "CONTRIBUTOR":
                            Contributor contributor = (Contributor) addContributions(user, jsonObj);
                            contributorList.add(contributor);
                            break;
                        case "ADMIN":
                            Admin admin = (Admin) addContributions(user, jsonObj);
                            adminList.add(admin);
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readActors(String path) {
        try {
            FileReader reader = new FileReader(path);
            char[] buffer = new char[1024];
            int bytesRead;
            StringBuilder content = new StringBuilder();
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(content.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                Actor newActor;
                if (!jsonObject.isNull("biography")) {
                    String biography = jsonObject.getString("biography");
                    newActor = new Actor(name, biography);
                } else {
                    newActor = new Actor(name);
                }
                JSONArray jsonProductions = jsonObject.getJSONArray("performances");

                for (int j = 0; j < jsonProductions.length(); j++) {
                    JSONObject jsonProduction = jsonProductions.getJSONObject(j);

                    String title = jsonProduction.getString("title");
                    String type = jsonProduction.getString("type");
                    newActor.addPerformance(title, type);
                }
                actors.add(newActor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readRequests(String path) {
        try {
            FileReader reader = new FileReader(path);
            char[] buffer = new char[1024];
            int bytesRead;
            StringBuilder content = new StringBuilder();
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(content.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String type = jsonObject.getString("type");
                String date = jsonObject.getString("createdDate");
                String creator = jsonObject.getString("username");
                String solver = jsonObject.getString("to");
                String description = jsonObject.getString("description");
                String title = "";
                if (!jsonObject.isNull("actorName")) {
                    title = jsonObject.getString("actorName");
                }
                if (!jsonObject.isNull("movieTitle")) {
                    title = jsonObject.getString("movieTitle");
                }
                boolean requestAdded = false;
                for (Regular regular : regularList) {
                    if (regular.getUsername().equals(creator)) {
                        regular.createRequest(new Request(type, date, description, creator, solver, title));
                        requestAdded = true;
                        break;
                    }
                }
                if (!requestAdded) {
                    for (Contributor contributor : contributorList) {
                        if (contributor.getUsername().equals(creator)) {
                            contributor.createRequest(new Request(type, date, description, creator, solver, title));
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readProductions(String path) {
        try {
            FileReader reader = new FileReader(path);
            char[] buffer = new char[1024];
            int bytesRead;
            StringBuilder content = new StringBuilder();
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(content.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String title = jsonObject.getString("title");
                String type = jsonObject.getString("type");
                String plot = jsonObject.getString("plot");
                Double avgRat = jsonObject.getDouble("averageRating");
                int relYr;
                if (!jsonObject.isNull("releaseYear")) {
                    relYr = jsonObject.getInt("releaseYear");
                } else {
                    relYr = -1;
                }
                if (type.equals("Movie")) {
                    String duration = jsonObject.getString("duration");
                    Movie movie = new Movie(title, plot, avgRat, relYr, duration);
                    JSONArray directors = jsonObject.getJSONArray("directors");
                    for (int j = 0; j < directors.length(); j++) {
                        movie.addDirector(directors.getString(j));
                    }
                    JSONArray actors = jsonObject.getJSONArray("actors");
                    for (int j = 0; j < actors.length(); j++) {
                        movie.addActor(actors.getString(j));
                    }
                    JSONArray genres = jsonObject.getJSONArray("genres");
                    for (int j = 0; j < genres.length(); j++) {
                        movie.addGenre(genres.getString(j));
                    }
                    JSONArray ratings = jsonObject.getJSONArray("ratings");
                    for (int j = 0; j < ratings.length(); j++) {
                        JSONObject jsonRat = ratings.getJSONObject(j);
                        Rating rating = new Rating(jsonRat.getString("username"), jsonRat.getString("comment"), jsonRat.getInt("rating"));
                        movie.addRating(rating);
                    }
                    movies.add(movie);
                } else {
                    int numSeasons = jsonObject.getInt("numSeasons");
                    Series newSeries = new Series(title, plot, avgRat, relYr, numSeasons);
                    JSONArray directors = jsonObject.getJSONArray("directors");
                    for (int j = 0; j < directors.length(); j++) {
                        newSeries.addDirector(directors.getString(j));
                    }
                    JSONArray actors = jsonObject.getJSONArray("actors");
                    for (int j = 0; j < actors.length(); j++) {
                        newSeries.addActor(actors.getString(j));
                    }
                    JSONArray genres = jsonObject.getJSONArray("genres");
                    for (int j = 0; j < genres.length(); j++) {
                        newSeries.addGenre(genres.getString(j));
                    }
                    JSONArray ratings = jsonObject.getJSONArray("ratings");
                    for (int j = 0; j < ratings.length(); j++) {
                        JSONObject jsonRat = ratings.getJSONObject(j);
                        Rating rating = new Rating(jsonRat.getString("username"), jsonRat.getString("comment"), jsonRat.getInt("rating"));
                        newSeries.addRating(rating);
                    }
                    JSONObject seasons = jsonObject.getJSONObject("seasons");
                    for (int j = 1; j <= numSeasons; j++) {
                        String seasonName = "Season " + j;
                        List<Episode> episodeList = new ArrayList<Episode>();
                        JSONArray episodes = seasons.getJSONArray(seasonName);
                        for (int k = 0; k < episodes.length(); k++) {
                            episodeList.add(new Episode(episodes.getJSONObject(k).getString("episodeName"), episodes.getJSONObject(k).getString("duration")));
                        }
                        newSeries.addSeason(seasonName, episodeList);
                    }
                    series.add(newSeries);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        //step 1
        // Pentru CMD
//        readActors("../../../../../../resources/input/actors.json");
//        readProductions("../../../../../../resources/input/production.json");
//        readUsers("../../../../../../resources/input/accounts.json");
//        readRequests("../../../../../../resources/input/requests.json");

        // Pentru IDE
        readActors("main/resources/input/actors.json");
        readProductions("main/resources/input/production.json");
        readUsers("main/resources/input/accounts.json");
        readRequests("main/resources/input/requests.json");

        for (Production production : movies){
            production.sortRatings();
            Regular user = null;
            for (Rating rating : production.getRatings()){
                for (Regular regular : regularList){
                    if (regular.getUsername().equals(rating.getUser())){
                        user = regular;
                        break;
                    }
                }
                if (user != null) production.getHasBeenReviewedBy().put(user, true);
            }
        }
        for (Production production : series){
            production.sortRatings();
            Regular user = null;
            for (Rating rating : production.getRatings()){
                for (Regular regular : regularList){
                    if (regular.getUsername().equals(rating.getUser())){
                        user = regular;
                        break;
                    }
                }
                if (user != null) production.getHasBeenReviewedBy().put(user, true);
            }
        }

        Object[] options = {"Open in Terminal", "Open with GUI"};
        clearConsole();

        // Display the dialog and get the user's choice
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose how to open the app:",
                "App Launcher",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case JOptionPane.YES_OPTION:
                // User chose to open in Terminal
                System.out.println("Opening in Terminal...");
                openInTerminal();
                break;
            case JOptionPane.NO_OPTION:
                // User chose to open with GUI
                openWithGUI();
                break;
            case JOptionPane.CLOSED_OPTION:
                System.out.println("Dialog closed");
                break;
        }
    }

    public final static void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                // For Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // For Unix-like systems
                new ProcessBuilder("bash", "-c", "clear").inheritIO().start().waitFor();
            }
        } catch (final Exception e) {
            System.out.println("Program does not support your operating system.");
            System.out.println("Exiting now");
            System.exit(0);
        }
    }

    public static User logIn() {
        User auxiliaryUser = null;
        while (auxiliaryUser == null) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome! Please enter your credentials.");
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            for (User user : regularList) {
                if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                    auxiliaryUser = user;
                }
            }
            for (User user : contributorList) {
                if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                    auxiliaryUser = user;
                }
            }
            for (User user : adminList) {
                if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                    auxiliaryUser = user;
                }
            }
            if (auxiliaryUser == null) {
                System.out.println("Wrong Credentials!");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                clearConsole();
            } else {
                return auxiliaryUser;
            }
        }
        return null;
    }

    //TERMINAL
    public static void openInTerminal() {
        clearConsole();
        User auxiliaryUser = null;
        Scanner scanner = new Scanner(System.in);
        // step 2
        if (auxiliaryUser == null) {
            auxiliaryUser = logIn();
        }
        //step 3
        switch (auxiliaryUser.getType()) {
            case AccountType.ADMIN:
                Admin currentAdmin = (Admin) auxiliaryUser;
                step3Admin(currentAdmin);
                break;
            case AccountType.CONTRIBUTOR:
                Contributor currentContributor = (Contributor) auxiliaryUser;
                step3Contributor(currentContributor);
                break;
            case AccountType.REGULAR:
                Regular currentUser = (Regular) auxiliaryUser;
                step3Regular(currentUser);
                break;
            default:
                scanner.close();
                System.exit(0);
                break;
        }
    }

    public static void step3Admin(Admin currentUser) {
        boolean isAccountLogged = true;
        Scanner scanner = new Scanner(System.in);
        while (isAccountLogged) {
            clearConsole();
            System.out.println("Welcome back " + currentUser.getUsername() + "!");
            System.out.println("Current Experience: " + currentUser.getExp());
            System.out.println("1) View productions details");
            System.out.println("2) View actors details");
            System.out.println("3) View notifications");
            System.out.println("4) Search for actor/movie/series");
            System.out.println("5) Add/Delete actor/movie/series to/from favorites");
            System.out.println("6) Add/Delete actor/movie/series in/from the system");
            System.out.println("7) Update Production details");
            System.out.println("8) Update Actor details");
            System.out.println("9) Add/Delete user");
            System.out.println("10) Solve a request");
            System.out.println("11) Log out");
            System.out.print("Choose Action: ");
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        viewProductionDetails();
                        break;
                    case 2:
                        viewActorsDetails();
                        scanner.nextLine();
                        break;
                    case 3:
                        viewNotifications(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 4:
                        findAMS(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 5:
                        addDeleteFavorites(currentUser);
                        break;
                    case 6:
                        addDeleteFromSystem(currentUser);
                        break;
                    case 7:
                        updateProduction(currentUser);
                        break;
                    case 8:
                        updateActor(currentUser);
                        break;
                    case 9:
                        createDeleteUser(currentUser);
                        break;
                    case 10:
                        solveRequest(currentUser);
                        break;
                    case 11:
                        clearConsole();
                        System.out.println("Do you want to:");
                        System.out.println("1) Log out");
                        System.out.println("2) Exit");
                        System.out.print("Choose action: ");
                        try {
                            int exitChoice = scanner.nextInt();
                            if (exitChoice == 1) {
                                isAccountLogged = false;
                                clearConsole();
                                openInTerminal();
                            } else if (exitChoice == 2) {
                                currentUser = null;
                                isAccountLogged = false;
                            } else {
                                System.out.println("Please input a number between 1 and 2.");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        } catch (InputMismatchException e) {
                            System.out.println("Please input a number between 1 and 2.");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    default:
                        System.out.println("Please input a number between 1 and 8.");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please input valid numbers.");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    public static void step3Contributor(Contributor currentUser) {
        boolean isAccountLogged = true;
        Scanner scanner = new Scanner(System.in);
        while (isAccountLogged) {
            clearConsole();
            System.out.println("Welcome back " + currentUser.getUsername() + "!");
            System.out.println("Current Experience: " + currentUser.getExp());
            System.out.println("1) View productions details");
            System.out.println("2) View actors details");
            System.out.println("3) View notifications");
            System.out.println("4) Search for actor/movie/series");
            System.out.println("5) Add/Delete actor/movie/series to/from favorites");
            System.out.println("6) Add/Delete actor/movie/series in/from the system");
            System.out.println("7) Update Production details");
            System.out.println("8) Update Actor details");
            System.out.println("9) Create/Retract Request");
            System.out.println("10) Solve a request");
            System.out.println("11) Log out");
            System.out.print("Choose Action: ");
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        viewProductionDetails();
                        break;
                    case 2:
                        viewActorsDetails();
                        scanner.nextLine();
                        break;
                    case 3:
                        viewNotifications(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 4:
                        findAMS(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 5:
                        addDeleteFavorites(currentUser);
                        break;
                    case 6:
                        addDeleteFromSystem(currentUser);
                        break;
                    case 7:
                        updateProduction(currentUser);
                        break;
                    case 8:
                        updateActor(currentUser);
                        break;
                    case 9:
                        createDeleteRequest(currentUser);
                        break;
                    case 10:
                        solveRequest(currentUser);
                        break;
                    case 11:
                        clearConsole();
                        System.out.println("Do you want to:");
                        System.out.println("1) Log out");
                        System.out.println("2) Exit");
                        System.out.print("Choose action: ");
                        try {
                            int exitChoice = scanner.nextInt();
                            if (exitChoice == 1) {
                                isAccountLogged = false;
                                clearConsole();
                                openInTerminal();
                            } else if (exitChoice == 2) {
                                currentUser = null;
                                isAccountLogged = false;
                            } else {
                                System.out.println("Please input a number between 1 and 2.");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        } catch (InputMismatchException e) {
                            System.out.println("Please input a number between 1 and 2.");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    default:
                        System.out.println("Please input a number between 1 and 8.");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please input valid numbers.");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    public static void step3Regular(Regular currentUser) {
        boolean isAccountLogged = true;
        Scanner scanner = new Scanner(System.in);
        while (isAccountLogged) {
            clearConsole();
            System.out.println("Welcome back " + currentUser.getUsername() + "!");
            System.out.println("Current Experience: " + currentUser.getExp());
            System.out.println("1) View productions details");
            System.out.println("2) View actors details");
            System.out.println("3) View notifications");
            System.out.println("4) Search for actor/movie/series");
            System.out.println("5) Add/Delete actor/movie/series to/from favorites");
            System.out.println("6) Create/Retract Request");
            System.out.println("7) Add/Delete a rating for a movie/series");
            System.out.println("8) Log out");
            System.out.print("Choose Action: ");
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        viewProductionDetails();
                        break;
                    case 2:
                        viewActorsDetails();
                        scanner.nextLine();
                        break;
                    case 3:
                        viewNotifications(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 4:
                        findAMS(currentUser);
                        System.out.print("Press Enter to go back.");
                        scanner.nextLine();
                        break;
                    case 5:
                        addDeleteFavorites(currentUser);
                        break;
                    case 6:
                        createDeleteRequest(currentUser);
                        break;
                    case 7:
                        clearConsole();
                        System.out.print("Please enter the movie/series title: ");
                        try {
                            String title = scanner.nextLine();
                            boolean didWeFinish = false;
                            for (Movie movie : movies) {
                                if (movie.getTitle().equals(title)) {
                                    didWeFinish = true;
                                    boolean found = false;
                                    for (Rating rating : movie.getRatings()) {
                                        if (rating.getUser().equals(currentUser.getUsername())) {
                                            found = true;
                                            System.out.print("You have already rated this movie. Do you want to delete your rating? [Y/N] : ");
                                            try {
                                                String answer = scanner.nextLine();
                                                if (answer.equals("Y")) {
                                                    movie.removeRating(rating);
                                                    System.out.println("Rating deleted successfully");
                                                } else if (!answer.equals("N")) {
                                                    System.out.println("Please put valid input");
                                                }
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            } catch (NoSuchElementException e) {
                                                System.out.println("Please input a valid answer.");
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            }
                                        }
                                    }
                                    if (!found) {
                                        System.out.print("Enter you rating (1-10): ");
                                        try {
                                            int newRating = scanner.nextInt();
                                            scanner.nextLine();
                                            if (newRating < 11 && newRating > 0) {
                                                System.out.print("Enter a comment: ");
                                                try {
                                                    String comment = scanner.nextLine();
                                                    currentUser.addRating(comment, newRating, movie);
                                                    System.out.println("Rating added Successfully!");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                    break;
                                                } catch (NoSuchElementException e) {
                                                    System.out.println("Please input a valid answer.");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                    break;
                                                }
                                            } else {
                                                System.out.println("Please input a valid answer.");
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            }
                                        } catch (InputMismatchException e) {
                                            System.out.println("Please input a valid number.");
                                            System.out.print("Press Enter to continue...");
                                            scanner.nextLine();
                                            break;
                                        }
                                    }
                                }
                            }
                            for (Series series1 : series) {
                                if (series1.getTitle().equals(title)) {
                                    didWeFinish = true;
                                    boolean found = false;
                                    for (Rating rating : series1.getRatings()) {
                                        if (rating.getUser().equals(currentUser.getUsername())) {
                                            found = true;
                                            System.out.print("You have already rated this series. Do you want to delete your rating? [Y/N] : ");
                                            try {
                                                String answer = scanner.nextLine();
                                                if (answer.equals("Y")) {
                                                    series1.removeRating(rating);
                                                    System.out.println("Rating deleted successfully");
                                                } else if (!answer.equals("N")) {
                                                    System.out.println("Please put valid input");
                                                }
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            } catch (NoSuchElementException e) {
                                                System.out.println("Please input a valid answer.");
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            }
                                        }
                                    }
                                    if (!found) {
                                        System.out.print("Enter you rating (1-10): ");
                                        try {
                                            int newRating = scanner.nextInt();
                                            scanner.nextLine();
                                            if (newRating < 11 && newRating > 0) {
                                                System.out.print("Enter a comment: ");
                                                try {
                                                    String comment = scanner.nextLine();
                                                    currentUser.addRating(comment, newRating, series1);
                                                    //series1.addRating(new Rating(currentUser.getUsername(), comment, newRating));
                                                    System.out.println("Rating added Successfully!");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                    break;
                                                } catch (NoSuchElementException e) {
                                                    System.out.println("Please input a valid answer.");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                    break;
                                                }
                                            } else {
                                                System.out.println("Please input a valid answer.");
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                                break;
                                            }
                                        } catch (InputMismatchException e) {
                                            System.out.println("Please input a valid number.");
                                            System.out.print("Press Enter to continue...");
                                            scanner.nextLine();
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!didWeFinish) {
                                System.out.println("Please put valid input");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        } catch (NoSuchElementException e) {
                            System.out.println("Please put valid input");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    case 8:
                        clearConsole();
                        System.out.println("Do you want to:");
                        System.out.println("1) Log out");
                        System.out.println("2) Exit");
                        System.out.print("Choose action: ");
                        try {
                            int exitChoice = scanner.nextInt();
                            scanner.nextLine();
                            if (exitChoice == 1) {
                                isAccountLogged = false;
                                clearConsole();
                                openInTerminal();
                            } else if (exitChoice == 2) {
                                currentUser = null;
                                isAccountLogged = false;
                            } else {
                                System.out.println("Please input a number between 1 and 2.");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        } catch (InputMismatchException e) {
                            System.out.println("Please input a number between 1 and 2.");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    default:
                        System.out.println("Please input a number between 1 and 8.");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please input valid numbers.");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    public static void main(String[] args) {
        IMDB instance = getInstance();
        instance.run();
    }

    public static void createDeleteUser(Admin currentUser) {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        System.out.println("1) Add a user in the system");
        System.out.println("2) Remove a user from the system");
        System.out.println("3) Go back");
        System.out.print("Choose action: ");
        int choice;
        try {
            choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    createUser();
                    break;
                case 2:
                    deleteUser(currentUser);
                    break;
                case 3:
                    break;
                default:
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        } catch (InputMismatchException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void createUser() {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the new user's email: ");
        String email = scanner.nextLine();
        System.out.print("Enter the new user's password length: ");
        try {
            int length = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Enter the new user's first name: ");
            String firstName = scanner.nextLine();
            System.out.print("Enter the new user's last name: ");
            String lastName = scanner.nextLine();
            System.out.print("Enter the new user's country: ");
            String country = scanner.nextLine();
            System.out.print("Enter the new user's age: ");
            try {
                int age = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter the new user's gender (M/F/N): ");
                String gender = scanner.nextLine();
                if (!gender.equals("M") && !gender.equals("F") && !gender.equals("N")){
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                }
                System.out.print("Enter the new user's date of birth (yyyy-mm-dd): ");
                String birthDate = scanner.nextLine();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                try {
                    LocalDate dateOfBirth = LocalDate.parse(birthDate, formatter);
                    String password = User.generatePassword(length);
                    User.Information information = User.Information.builder()
                            .credentials(User.Information.credentialBuilder()
                                    .email(email)
                                    .password(password)
                                    .build())
                            .name(firstName + " " + lastName)
                            .country(country)
                            .age(age)
                            .gender(gender)
                            .birthDate(dateOfBirth)
                            .build();

                    boolean correctInput;
                    do {
                        correctInput = true;
                        System.out.print("Enter the new user's type (REGULAR/CONTRIBUTOR/ADMIN): ");
                        String type = scanner.nextLine();
                        String username = User.generateUsername(firstName, lastName);
                        switch (type.toUpperCase()) {
                            case "REGULAR":
                                Regular regular = new Regular(information, username, 0);
                                regularList.add(regular);
                                for (Production production : movies) {
                                    production.addRegularsToReviews(regular);
                                }
                                for (Production production : series) {
                                    production.addRegularsToReviews(regular);
                                }
                                clearConsole();
                                System.out.println("Regular user " + username + " has been added in the system");
                                System.out.println("Email: " + email);
                                System.out.println("Password: " + password);
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                                break;
                            case "CONTRIBUTOR":
                                contributorList.add(new Contributor(information, username, 0));
                                clearConsole();
                                System.out.println("Contributor " + username + " has been added in the system");
                                System.out.println("Email: " + email);
                                System.out.println("Password: " + password);
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                                break;
                            case "ADMIN":
                                adminList.add(new Admin(information, username, 0));
                                clearConsole();
                                System.out.println("Admin " + username + " has been added in the system");
                                System.out.println("Email: " + email);
                                System.out.println("Password: " + password);
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                                break;
                            default:
                                correctInput = false;
                                break;
                        }
                    } while (!correctInput);
                } catch (DateTimeParseException e) {
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                }
            } catch (InputMismatchException e) {
                System.out.println("Please put valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        } catch (InputMismatchException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            scanner.nextLine();
        }
    }

    public static void deleteUser(Admin currentUser) {
        Scanner scanner = new Scanner(System.in);
        clearConsole();
        System.out.print("Regular users: ");
        int count = 0;
        for (Regular user : regularList) {
            count++;
            if (count % 10 == 0) {
                System.out.print("\n");
            }
            System.out.print(user.getUsername());
            if (!user.equals(regularList.getLast())) {
                System.out.print(", ");
            }
        }
        System.out.println();
        System.out.print("Contributors: ");
        count = 0;
        for (Contributor user : contributorList) {
            count++;
            if (count % 10 == 0) {
                System.out.print("\n");
            }
            System.out.print(user.getUsername());
            if (!user.equals(contributorList.getLast())) {
                System.out.print(", ");
            }
        }
        System.out.println();
        System.out.print("Admins: ");
        count = 0;
        for (Admin user : adminList) {
            count++;
            if (!user.equals(currentUser)) {
                if (count % 10 == 0) {
                    System.out.print("\n");
                }
                System.out.print(user.getUsername());
                if (!user.equals(adminList.getLast())) {
                    System.out.print(", ");
                }
            }
        }
        System.out.println();
        System.out.println();
        System.out.print("Choose which user to delete: ");
        String name = scanner.nextLine();
        boolean foundAndDeleted = false;
        if (name.isEmpty()) System.out.println("Please put valid input");
        else if (name.equals(currentUser.getUsername())) {
            System.out.println("You can't remove yourslef!");
        } else {
            foundAndDeleted = currentUser.removeUser(name);
            if (foundAndDeleted) System.out.println("User removed successfully!");
            else System.out.println("User does not exist in the system");
        }
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    public static void solveRequest(Staff currentUser) {
        Scanner scanner = new Scanner(System.in);
        clearConsole();
        int requestCounter = 0;
        if (currentUser instanceof Admin) {
            for (Request r : Admin.RequestsHolder.getTeamRequests()) {
                requestCounter++;
                System.out.print(requestCounter + ") Request made by " + r.getCreatorUsername() + " at " + r.getTime().toString() + " regarding ");
                switch (r.getRequestType()) {
                    case ACTOR_ISSUE:
                        System.out.print("an issue with an actor (" + r.getTitleOrMovie() + ")\n");
                        break;
                    case MOVIE_ISSUE:
                        System.out.print("an issue with a production (" + r.getTitleOrMovie() + ")\n");
                        break;
                    case DELETE_ACCOUNT:
                        System.out.print("the deletion of an account\n");
                        break;
                    case OTHERS:
                        System.out.print("other reasons\n");
                        break;
                }
                System.out.println("Description: " + r.getDescription());
                System.out.println();
            }
        }
        for (Request r : currentUser.getOwnList()) {
            requestCounter++;
            System.out.print(requestCounter + ") Request made by " + r.getCreatorUsername() + " at " + r.getTime().toString() + " regarding ");
            switch (r.getRequestType()) {
                case ACTOR_ISSUE:
                    System.out.print("an issue with an actor (" + r.getTitleOrMovie() + ")\n");
                    break;
                case MOVIE_ISSUE:
                    System.out.print("an issue with a production (" + r.getTitleOrMovie() + ")\n");
                    break;
                case DELETE_ACCOUNT:
                    System.out.print("the deletion of an account\n");
                    break;
                case OTHERS:
                    System.out.print("other reasons\n");
                    break;
            }
            System.out.println("Description: " + r.getDescription());
            System.out.println();
        }
        if (requestCounter == 0) {
            System.out.println("No requests to solve!");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        System.out.println();
        System.out.print("Choose which request to solve, or type 0 to go back: ");
        Request requestToMark = null;
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();
            if (choice == 0) return;
            if (currentUser instanceof Admin) {
                if (choice > 0 && choice <= Admin.RequestsHolder.getTeamRequests().size()) {
                    requestToMark = Admin.RequestsHolder.getTeamRequests().get(choice - 1);
                } else if (choice > Admin.RequestsHolder.getTeamRequests().size() && choice <= Admin.RequestsHolder.getTeamRequests().size() + currentUser.getOwnList().size()) {
                    requestToMark = currentUser.getOwnList().get(choice - 1 - Admin.RequestsHolder.getTeamRequests().size());
                } else {
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
            } else {
                if (choice > 0 && choice <= currentUser.getOwnList().size()) {
                    requestToMark = currentUser.getOwnList().get(choice - 1);
                } else {
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
            }
            System.out.println("1) Mark request as SOLVED");
            System.out.println("2) Mark request as REJECTED");
            System.out.print("Choose action: ");
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
                if (choice == 1) {
                    currentUser.solveRequest(requestToMark, "SOLVED");
                } else if (choice == 2) {
                    currentUser.solveRequest(requestToMark, "REJECTED");
                } else {
                    System.out.println("Please put valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
                System.out.println("Request marked successfully");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            } catch (IllegalArgumentException e) {
                System.out.println("Please put valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void updateProduction(Staff currentUser) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Movies: ");
        int count = 0;
        List<String> productionNames = new ArrayList<>();
        for (Production production : currentUser.getProductionsAdded()) {
            count++;
            System.out.print(production.getTitle());
            productionNames.add(production.getTitle());
            if (!production.equals(currentUser.getProductionsAdded().getLast())) {
                System.out.print(", ");
            }
            if (count % 10 == 0) {
                System.out.print("\n");
            }
        }
        System.out.println();
        System.out.print("Choose the production which you want to update: ");
        String title = scanner.nextLine();
        if (!productionNames.contains(title)) {
            System.out.println("Production either doesn't exist or you did not add it in the system");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        Production productionToUpdate = null;
        for (Production production : currentUser.getProductionsAdded()) {
            if (production.getTitle().equals(title)) {
                productionToUpdate = production;
            }
        }
        boolean isFinishedEditing = false;
        while (!isFinishedEditing) {
            clearConsole();
            System.out.println("1) Change plot");
            System.out.println("2) Change release year");
            System.out.println("3) Add director");
            System.out.println("4) Remove Director");
            System.out.println("5) Add Actor");
            System.out.println("6) Remove Actor");
            System.out.println("7) Add Genre");
            System.out.println("8) Remove Genre");
            if (productionToUpdate instanceof Movie) {
                System.out.println("9) Change duration");
            } else {
                System.out.println("9) Add season");
            }
            System.out.println("10) Finish");
            System.out.print("Choose action: ");
            int choice = 0;
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        clearConsole();
                        System.out.print("New plot: ");
                        String newPlot = scanner.nextLine();
                        productionToUpdate.setPlot(newPlot);
                        System.out.println("Plot changed successfully!");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 2:
                        clearConsole();
                        System.out.print("Enter new release year: ");
                        try {
                            int newReleaseYear = scanner.nextInt();
                            scanner.nextLine();
                            if (productionToUpdate instanceof Movie) {
                                ((Movie) productionToUpdate).setReleaseYear(newReleaseYear);
                            } else {
                                ((Series) productionToUpdate).setReleaseYear(newReleaseYear);
                            }
                            System.out.println("Release year changed successfully");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        } catch (InputMismatchException e) {
                            System.out.println("Please provide valid input");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                            scanner.nextLine();
                        }
                        break;
                    case 3:
                        clearConsole();
                        System.out.print("Enter new director: ");
                        String name = scanner.nextLine();
                        if (productionToUpdate.getDirectors().contains(name)) {
                            System.out.println("Director already in list");
                        } else {
                            productionToUpdate.addDirector(name);
                            System.out.println("Director added successfully!");
                        }
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 4:
                        clearConsole();
                        System.out.print("Current directors: ");
                        count = 0;
                        for (String director : productionToUpdate.getDirectors()) {
                            count++;
                            System.out.print(director);
                            if (!director.equals(productionToUpdate.getDirectors().getLast())) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Enter the name of the director to be deleted: ");
                        String directorName = scanner.nextLine();
                        if (productionToUpdate.getDirectors().contains(directorName)) {
                            productionToUpdate.removeDirector(directorName);
                            System.out.println("Director removed successfully");
                        } else {
                            System.out.println("Director is not present in the director list");
                        }
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 5:
                        clearConsole();
                        System.out.print("Enter new actor: ");
                        String actorName = scanner.nextLine();
                        if (productionToUpdate.getActors().contains(actorName)) {
                            System.out.println("Actor already in list");
                        } else {
                            productionToUpdate.addActor(actorName);
                            System.out.println("Actor added successfully!");
                        }
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 6:
                        clearConsole();
                        System.out.print("Current actors: ");
                        count = 0;
                        for (String actor : productionToUpdate.getActors()) {
                            count++;
                            System.out.print(actor);
                            if (!actor.equals(productionToUpdate.getActors().getLast())) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Enter the name of the actor to be deleted: ");
                        String nameOfActor = scanner.nextLine();
                        if (productionToUpdate.getActors().contains(nameOfActor)) {
                            productionToUpdate.removeDirector(nameOfActor);
                            System.out.println("Actor removed successfully");
                        } else {
                            System.out.println("Actor is not present in the director list");
                        }
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 7:
                        clearConsole();
                        System.out.print("Current genres: ");
                        count = 0;
                        for (Genre genre : productionToUpdate.getGenres()) {
                            count++;
                            System.out.print(genre.toString());
                            if (!genre.equals(productionToUpdate.getGenres().getLast())) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Available genres: ");
                        for (Genre genre : Genre.values()) {
                            count++;
                            System.out.print(genre.toString());
                            if (!genre.equals(Genre.values()[Genre.values().length - 1])) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Enter a genre to be added: ");
                        String genreToBeAdded = scanner.nextLine();
                        try {
                            if (productionToUpdate.getGenres().contains(Genre.valueOf(genreToBeAdded))) {
                                System.out.println("Genre is already in the list");
                            } else {
                                productionToUpdate.addGenre(genreToBeAdded);
                                System.out.println("Genre added successfully");
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Please input one of the available genres");
                        }
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 8:
                        clearConsole();
                        System.out.print("Current genres: ");
                        count = 0;
                        for (Genre genre : productionToUpdate.getGenres()) {
                            count++;
                            System.out.print(genre.toString());
                            if (!genre.equals(productionToUpdate.getGenres().getLast())) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Available genres: ");
                        for (Genre genre : Genre.values()) {
                            count++;
                            System.out.print(genre.toString());
                            if (!genre.equals(Genre.values()[Genre.values().length - 1])) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Enter a genre to be removed: ");
                        String genreToBeRemoved = scanner.nextLine();
                        productionToUpdate.removeGenre(genreToBeRemoved);
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 9:
                        clearConsole();
                        if (productionToUpdate instanceof Movie) {
                            System.out.print("Current duration: ");
                            System.out.print(((Movie) productionToUpdate).getMovieLength() + "\n");
                            System.out.print("New duration: ");
                            String newDuration = scanner.nextLine();
                            ((Movie) productionToUpdate).setMovieLength(newDuration);
                            System.out.println("Duration changed successfully");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        } else {
                            ((Series) productionToUpdate).setSeasonsNumber(((Series) productionToUpdate).getSeasonsNumber() + 1);
                            System.out.print("Enter the season's name: ");
                            String seasonName = scanner.nextLine();
                            int numberOfEpisodes = 0;
                            try {
                                do {
                                    System.out.print("Enter the number of episodes: ");
                                    numberOfEpisodes = scanner.nextInt();
                                    scanner.nextLine();
                                    if (numberOfEpisodes < 0) {
                                        System.out.println("Please input a positive number");
                                    }
                                } while (numberOfEpisodes < 0);
                            } catch (InputMismatchException e) {
                                System.out.println("Please provide valid input");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            } finally {
                                List<Episode> episodeList = new ArrayList<>();
                                for (int i = 1; i <= numberOfEpisodes; i++) {
                                    System.out.print("Enter the ");
                                    if (i % 10 == 1) {
                                        System.out.print(i + "st ");
                                    } else if (i % 10 == 2) {
                                        System.out.print(i + "nd ");
                                    } else if (i % 10 == 2) {
                                        System.out.print(i + "rd ");
                                    } else {
                                        System.out.print(i + "th ");
                                    }
                                    System.out.print("episode's name: ");
                                    String episodeName = scanner.nextLine();
                                    System.out.print("Enter the episode's length: ");
                                    String episodeLength = scanner.nextLine();
                                    episodeList.add(new Episode(episodeName, episodeLength));
                                }
                                ((Series) productionToUpdate).addSeason(seasonName, episodeList);
                                System.out.println("Season added successfully!");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        }
                        break;
                    case 10:
                        isFinishedEditing = true;
                        clearConsole();
                        System.out.println("Production updated successfully!");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    default:
                        System.out.println("Please provide valid input");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please provide valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
                scanner.nextLine();
            }
        }
    }

    public static void updateActor(Staff currentUser) {
        Scanner scanner = new Scanner(System.in);
        clearConsole();
        System.out.print("Actors: ");
        int count = 0;
        List<String> actorNames = new ArrayList<>();
        for (Actor actor : currentUser.getActorsAdded()) {
            count++;
            actorNames.add(actor.getActorName());
            System.out.print(actor.getActorName());
            if (!actor.equals(currentUser.getActorsAdded().getLast())) {
                System.out.print(", ");
            }
            if (count % 10 == 0) {
                System.out.println();
            }
        }
        System.out.println();
        System.out.print("Choose which actor to update: ");
        String name = scanner.nextLine();
        if (!actorNames.contains(name)) {
            System.out.println("Actor either doesn't exist or you did not add it in the system");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        Actor actorToEdit = null;
        for (Actor actor : currentUser.getActorsAdded()) {
            if (actor.getActorName().equals(name)) {
                actorToEdit = actor;
                break;
            }
        }
        boolean isFinishedEditing = false;
        while (!isFinishedEditing) {
            clearConsole();
            System.out.println("1) Change biography");
            System.out.println("2) Add performance");
            System.out.println("3) Remove performance");
            System.out.println("4) Finish editing");
            System.out.print("Choose action: ");
            int choice = 0;
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        clearConsole();
                        System.out.print("New Biography: ");
                        String newBio = scanner.nextLine();
                        actorToEdit.setBiography(newBio);
                        System.out.println("Biography changed successfully!");
                        System.out.println("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 2:
                        clearConsole();
                        System.out.print("Enter the production name: ");
                        String productionName = scanner.nextLine();
                        System.out.print("Enter the production type (Movie/Series): ");
                        String productionType = scanner.nextLine();
                        if (productionType.equals("Movie") || productionType.equals("Series")) {
                            actorToEdit.addPerformance(productionName, productionType);
                        } else {
                            System.out.println("Please specify if the production is either a MOVIE or a SERIES");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    case 3:
                        clearConsole();
                        System.out.print("Current Performances: ");
                        count = 0;
                        String lastKey = "";
                        for (String key : actorToEdit.getPerformances().keySet()) {
                            lastKey = key;
                        }
                        for (String performance : actorToEdit.getPerformances().keySet()) {
                            count++;
                            System.out.print(performance);
                            if (!performance.equals(lastKey)) {
                                System.out.print(", ");
                            }
                            if (count % 10 == 0) {
                                System.out.print("\n");
                            }
                        }
                        System.out.println();
                        System.out.print("Choose which performance to remove: ");
                        String performanceToRemove = scanner.nextLine();
                        if (actorToEdit.getPerformances().containsKey(performanceToRemove)) {
                            actorToEdit.removePerformance(performanceToRemove);
                            System.out.println("Performance removed successfully.");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        }
                        break;
                    case 4:
                        isFinishedEditing = true;
                        break;
                    default:
                        System.out.println("Please provide valid input");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please provide valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
                scanner.nextLine();
            }
        }
        currentUser.updateActor(actorToEdit);
        clearConsole();
        System.out.println("Actor updated successfully!");
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    public static void addDeleteFromSystem(Staff currentUser) {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        boolean wrongInput;
        do {
            wrongInput = false;
            try {
                System.out.println("1) Add Production");
                System.out.println("2) Add Actor");
                System.out.println("3) Delete Production");
                System.out.println("4) Delete Actor");
                System.out.println("5) Back");
                System.out.print("Choose action: ");
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        clearConsole();
                        System.out.print("Enter the name of the production: ");
                        String prodName = scanner.nextLine();
                        System.out.print("Enter the synopsis of the production: ");
                        String plot = scanner.nextLine();
                        int releaseYear = 0;
                        System.out.print("Enter the release year: ");
                        try {
                            releaseYear = scanner.nextInt();
                            scanner.nextLine();
                        } catch (InputMismatchException e) {
                            System.out.println("Please put valid input");
                            System.out.print("Press Enter to continue...");
                            scanner.nextLine();
                        } finally {
                            System.out.println("What type of production is it?");
                            System.out.println("1) Movie");
                            System.out.println("2) Series");
                            System.out.print("Choose the type: ");
                            int typeChoice;
                            try {
                                typeChoice = scanner.nextInt();
                                scanner.nextLine();
                                switch (typeChoice) {
                                    case 1:
                                        System.out.print("Enter the length of the movie: ");
                                        String movieLength = scanner.nextLine();
                                        Movie newMovie = new Movie(prodName, plot, releaseYear, movieLength);
                                        boolean addingCast = true;
                                        while (addingCast) {
                                            clearConsole();
                                            System.out.println("1) Add director");
                                            System.out.println("2) Add actors");
                                            System.out.println("3) Add Genre");
                                            System.out.println("4) Continue");
                                            System.out.print("Choose action: ");
                                            try {
                                                int addChoice = scanner.nextInt();
                                                scanner.nextLine();
                                                switch (addChoice) {
                                                    case 1:
                                                        clearConsole();
                                                        System.out.print("Enter the director's name: ");
                                                        String directorName = scanner.nextLine();
                                                        newMovie.addDirector(directorName);
                                                        break;
                                                    case 2:
                                                        clearConsole();
                                                        System.out.print("Enter the actor's name: ");
                                                        String actorName = scanner.nextLine();
                                                        newMovie.addActor(actorName);
                                                        break;
                                                    case 3:
                                                        clearConsole();
                                                        int count = 0;
                                                        for (Genre genre : Genre.values()) {
                                                            count++;
                                                            System.out.print(genre.toString());
                                                            if (!genre.equals(Genre.values()[Genre.values().length - 1])) {
                                                                System.out.print(", ");
                                                            }
                                                            if (count % 10 == 0) {
                                                                System.out.print("\n");
                                                            }
                                                        }
                                                        System.out.println();
                                                        System.out.print("Enter the genre which to add: ");
                                                        String genreAdded = scanner.nextLine();
                                                        try {
                                                            Genre genreCheck = Genre.valueOf(genreAdded);
                                                            newMovie.getGenres().add(genreCheck);
                                                        } catch (IllegalArgumentException e) {
                                                            System.out.println("Invalid Genre! Try again");
                                                        }
                                                        break;
                                                    case 4:
                                                        addingCast = false;
                                                        break;
                                                    default:
                                                        System.out.println("Please put valid input");
                                                        System.out.print("Press Enter to continue...");
                                                        scanner.nextLine();
                                                        break;
                                                }
                                            } catch (InputMismatchException e) {
                                                System.out.println("Please put valid input");
                                                System.out.print("Press Enter to continue...");
                                                scanner.nextLine();
                                            }
                                        }
                                        clearConsole();
                                        currentUser.addProductionSystem(newMovie);
                                        System.out.println("Movie added successfully!");
                                        System.out.print("Press Enter to continue...");
                                        scanner.nextLine();
                                        break;
                                    case 2:
                                        int numberOfSeasons = 0;
                                        try {
                                            do {
                                                System.out.print("Enter the number of seasons: ");
                                                numberOfSeasons = scanner.nextInt();
                                                scanner.nextLine();
                                                if (numberOfSeasons < 0) {
                                                    System.out.println("Please input a positive number");
                                                }
                                            } while (numberOfSeasons < 0);
                                        } catch (InputMismatchException e) {
                                            System.out.println("Please put valid input");
                                            System.out.print("Press Enter to continue...");
                                            scanner.nextLine();
                                        } finally {
                                            Series newSeries = new Series(prodName, plot, releaseYear, numberOfSeasons);
                                            boolean addingCast1 = true;
                                            while (addingCast1) {
                                                clearConsole();
                                                System.out.println("1) Add director");
                                                System.out.println("2) Add actors");
                                                System.out.println("3) Add Genre");
                                                System.out.println("4) Continue");
                                                System.out.print("Choose action: ");
                                                try {
                                                    int addChoice = scanner.nextInt();
                                                    scanner.nextLine();
                                                    switch (addChoice) {
                                                        case 1:
                                                            clearConsole();
                                                            System.out.print("Enter the director's name: ");
                                                            String directorName = scanner.nextLine();
                                                            newSeries.addDirector(directorName);
                                                            break;
                                                        case 2:
                                                            clearConsole();
                                                            System.out.print("Enter the actor's name: ");
                                                            String actorName = scanner.nextLine();
                                                            newSeries.addActor(actorName);
                                                            break;
                                                        case 3:
                                                            clearConsole();
                                                            int count = 0;
                                                            for (Genre genre : Genre.values()) {
                                                                count++;
                                                                System.out.print(genre.toString());
                                                                if (!genre.equals(Genre.values()[Genre.values().length - 1])) {
                                                                    System.out.print(", ");
                                                                }
                                                                if (count % 10 == 0) {
                                                                    System.out.print("\n");
                                                                }
                                                            }
                                                            System.out.println();
                                                            System.out.print("Enter the genre which to add: ");
                                                            String genreAdded = scanner.nextLine();
                                                            try {
                                                                Genre genreCheck = Genre.valueOf(genreAdded);
                                                                newSeries.getGenres().add(genreCheck);
                                                            } catch (IllegalArgumentException e) {
                                                                System.out.println("Invalid Genre! Try again");
                                                            }
                                                            break;
                                                        case 4:
                                                            addingCast1 = false;
                                                            break;
                                                        default:
                                                            System.out.println("Please put valid input");
                                                            System.out.print("Press Enter to continue...");
                                                            scanner.nextLine();
                                                            break;
                                                    }
                                                } catch (InputMismatchException e) {
                                                    System.out.println("Please put valid input");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                }
                                            }
                                            for (int i = 1; i <= numberOfSeasons; i++) {
                                                clearConsole();
                                                System.out.print("Enter the ");
                                                if (i % 10 == 1) {
                                                    System.out.print(i + "st ");
                                                } else if (i % 10 == 2) {
                                                    System.out.print(i + "nd ");
                                                } else if (i % 10 == 3) {
                                                    System.out.print(i + "rd ");
                                                } else {
                                                    System.out.print(i + "th ");
                                                }
                                                System.out.print("season's name: ");
                                                String seasonName = scanner.nextLine();
                                                int numberOfEpisodes = 0;
                                                List<Episode> episodeList = new ArrayList<>();
                                                try {
                                                    do {
                                                        System.out.print("Enter the number of episodes: ");
                                                        numberOfEpisodes = scanner.nextInt();
                                                        scanner.nextLine();
                                                        if (numberOfEpisodes < 0) {
                                                            System.out.println("Please input a positive number");
                                                        }
                                                    } while (numberOfEpisodes < 0);
                                                } catch (InputMismatchException e) {
                                                    System.out.println("Please put valid input");
                                                    System.out.print("Press Enter to continue...");
                                                    scanner.nextLine();
                                                } finally {
                                                    for (int j = 1; j <= numberOfEpisodes; j++) {
                                                        System.out.print("Enter the ");
                                                        if (j % 10 == 1) {
                                                            System.out.print(j + "st ");
                                                        } else if (j % 10 == 2) {
                                                            System.out.print(j + "nd ");
                                                        } else if (j % 10 == 3) {
                                                            System.out.print(j + "rd ");
                                                        } else {
                                                            System.out.print(j + "th ");
                                                        }
                                                        System.out.print("episode's name: ");
                                                        String episodeName = scanner.nextLine();
                                                        System.out.print("Enter the episode's length: ");
                                                        String episodeLength = scanner.nextLine();
                                                        episodeList.add(new Episode(episodeName, episodeLength));
                                                    }
                                                    newSeries.addSeason(seasonName, episodeList);
                                                }
                                            }
                                            currentUser.addProductionSystem(newSeries);
                                            series.add(newSeries);
                                        }
                                        clearConsole();
                                        System.out.println("Series added Successfully!");
                                        System.out.print("Press Enter to continue...");
                                        scanner.nextLine();
                                        break;
                                    default:
                                        break;
                                }
                            } catch (InputMismatchException e) {
                                System.out.println("Please put valid input");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                                scanner.nextLine();
                            }
                        }
                        break;
                    case 2:
                        clearConsole();
                        System.out.print("Enter the name of the actor: ");
                        String actorName = scanner.nextLine();
                        System.out.print("Enter the biography of the actor: ");
                        String biography = scanner.nextLine();
                        Actor newActor = new Actor(actorName, biography);
                        while (true) {
                            clearConsole();
                            System.out.println("1) Add performance");
                            System.out.println("2) Finish");
                            System.out.print("Choose action: ");
                            try {
                                int option = scanner.nextInt();
                                scanner.nextLine();
                                if (option == 2) break;
                                else if (option == 1) {
                                    clearConsole();
                                    System.out.print("Enter the production name: ");
                                    String productionName = scanner.nextLine();
                                    System.out.print("Enter the production type (Movie/Series): ");
                                    String productionType = scanner.nextLine();
                                    if (productionType.equals("Movie") || productionType.equals("Series")) {
                                        newActor.addPerformance(productionName, productionType);
                                    } else {
                                        System.out.println("Please specify if the production is either a MOVIE or a SERIES");
                                        System.out.print("Press Enter to continue...");
                                        scanner.nextLine();
                                    }
                                } else {
                                    System.out.println("Please input either 1 or 2");
                                    System.out.print("Press Enter to continue...");
                                    scanner.nextLine();
                                }
                            } catch (InputMismatchException e) {
                                System.out.println("Please input either 1 or 2");
                                System.out.print("Press Enter to continue...");
                                scanner.nextLine();
                            }
                        }
                        currentUser.addActorSystem(newActor);
                        actors.add(newActor);
                        break;
                    case 3:
                        clearConsole();
                        int count = 0;
                        System.out.print("Productions: ");
                        List<String> productionTitles = new ArrayList<>();
                        for (Production production : currentUser.getProductionsAdded()) {
                            count++;
                            System.out.print(production.getTitle());
                            productionTitles.add(production.getTitle());
                            if (!production.equals(currentUser.getProductionsAdded().getLast())) {
                                System.out.print(", ");
                            }
                            if (count % 5 == 0) {
                                System.out.println();
                            }
                        }
                        System.out.println("\n");
                        System.out.print("Choose which production to delete: ");
                        String title = scanner.nextLine();
                        if (productionTitles.contains(title)) {
                            currentUser.removeProductionSystem(title);
                            System.out.println("Production removed successfully");
                        } else
                            System.out.println("Production either doesn't exist or you did not add it in the system");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 4:
                        clearConsole();
                        System.out.print("Actors: ");
                        List<String> actorList = new ArrayList<>();
                        for (Actor actor : currentUser.getActorsAdded()) {
                            System.out.print(actor.getActorName());
                            actorList.add(actor.getActorName());
                            if (!actor.equals(currentUser.getActorsAdded().getLast())) {
                                System.out.print(", ");
                            }
                        }
                        System.out.println("\n");
                        System.out.print("Choose which actor to delete: ");
                        String name = scanner.nextLine();
                        if (actorList.contains(name)) {
                            currentUser.removeActorSystem(name);
                            System.out.println("Actor removed successfully");
                        } else System.out.println("Actor either doesn't exist or you did not add it in the system");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                    case 5:
                        break;
                    default:
                        wrongInput = true;
                        System.out.println("Please input numbers between 1 and 4");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please input valid numbers.");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        } while (wrongInput);
    }

    public static void viewActorsDetails() {
        clearConsole();
        for (Actor actor : actors) {
            actor.displayInfo();
            System.out.println();
            System.out.println();
        }
        System.out.print("Press Enter to continue...");
    }

    public static void viewProductionDetails() {
        Scanner scanner = new Scanner(System.in);
        clearConsole();
        System.out.println("1) View Movies");
        System.out.println("2) View Series");
        System.out.println("3) Go back");
        System.out.print("Choose Action: ");
        try {
            int choiceProdDetails = scanner.nextInt();
            scanner.nextLine();
            switch (choiceProdDetails) {
                case 1:
                    for (Movie movie : movies) {
                        movie.displayInfo();
                        System.out.println();
                        System.out.println();
                    }
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case 2:
                    for (Series serie : series) {
                        serie.displayInfo();
                        System.out.println();
                        System.out.println();
                    }
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case 3:
                    break;
                default:
                    System.out.println("Please input a number between 1 and 3.");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        } catch (InputMismatchException e) {
            System.out.println("Please input a number between 1 and 3.");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void createDeleteRequest(User currentUser) {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        System.out.println("1) Create a Request");
        System.out.println("2) Delete a Request");
        System.out.print("Choose option: ");
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    createR(currentUser);
                    break;
                case 2:
                    removeR(currentUser);
                    break;
                default:
                    System.out.println("Please enter 1 or 2");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        } catch (InputMismatchException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void createR(User currentUser) {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        System.out.println("What type of request do you have?");
        System.out.println("1) Actor Issue");
        System.out.println("2) Production Issue");
        System.out.println("3) Account removal");
        System.out.println("4) Other");
        System.out.print("Choose option: ");
        String requestType = "";
        String description;
        String title = "";
        String solverUsername = "";
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    requestType = RequestTypes.ACTOR_ISSUE.toString();
                    break;
                case 2:
                    requestType = RequestTypes.MOVIE_ISSUE.toString();
                    break;
                case 3:
                    requestType = RequestTypes.DELETE_ACCOUNT.toString();
                    break;
                case 4:
                    requestType = RequestTypes.OTHERS.toString();
                    break;
                default:
                    System.out.println("Please choose a valid option");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
            }
            if (requestType.equals(RequestTypes.MOVIE_ISSUE.toString()) || requestType.equals(RequestTypes.ACTOR_ISSUE.toString())) {
                clearConsole();
                if (requestType.equals(RequestTypes.ACTOR_ISSUE.toString()))
                    System.out.print("Please enter the name of the actor: ");
                else System.out.print("Please enter the title of the production: ");
                try {
                    title = scanner.nextLine();
                    clearConsole();
                } catch (NoSuchElementException e) {
                    System.out.println("Please enter valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
            }
            try {
                System.out.print("Explain your problem: ");
                description = scanner.nextLine();
                if (!title.isEmpty()) {
                    for (Contributor contributor : contributorList) {
                        for (Production production : contributor.getProductionsAdded()) {
                            if (title.equals(production.getTitle())) {
                                solverUsername = contributor.getUsername();
                                break;
                            }
                        }
                        if (!solverUsername.isEmpty()) break;
                        for (Actor actor : contributor.getActorsAdded()) {
                            if (title.equals(actor.getActorName())) {
                                solverUsername = contributor.getUsername();
                                break;
                            }
                        }
                        if (!solverUsername.isEmpty()) break;
                    }
                    for (Admin admin : adminList) {
                        for (Production production : admin.getProductionsAdded()) {
                            if (title.equals(production.getTitle())) {
                                solverUsername = admin.getUsername();
                                break;
                            }
                        }
                        if (!solverUsername.isEmpty()) break;
                        for (Actor actor : admin.getActorsAdded()) {
                            if (title.equals(actor.getActorName())) {
                                solverUsername = admin.getUsername();
                                break;
                            }
                        }
                        if (!solverUsername.isEmpty()) break;
                    }
                    if (solverUsername.isEmpty()) {
                        if (requestType.equals(RequestTypes.ACTOR_ISSUE.toString()))
                            System.out.println("Actor not found");
                        else System.out.println("Production not found");
                        System.out.print("Press Enter to continue...");
                        scanner.nextLine();
                        return;
                    }
                } else {
                    solverUsername = "ADMIN";
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                if (solverUsername.equals(currentUser.getUsername())) {
                    System.out.println("You can't create a request addressed to yourself");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
                if (currentUser instanceof Regular) {
                    ((Regular) currentUser).createRequest(new Request(requestType, LocalDateTime.now().format(formatter), description, currentUser.getUsername(), solverUsername, title));
                } else {
                    ((Contributor) currentUser).createRequest(new Request(requestType, LocalDateTime.now().format(formatter), description, currentUser.getUsername(), solverUsername, title));
                }
                System.out.println("Request created successfully! Check your notifications regularly to see if it was solved.");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            } catch (NoSuchElementException e) {
                System.out.println("Please enter valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        } catch (InputMismatchException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void removeR(User currentUser) {
        List<Request> RequestsMadeByMe = getMyRequests(currentUser);
        Scanner scanner = new Scanner(System.in);
        if (!RequestsMadeByMe.isEmpty()) {
            for (int i = 0; i < RequestsMadeByMe.size(); i++) {
                Request r = RequestsMadeByMe.get(i);
                System.out.print(i + 1 + ") Request addressed to ");
                if (r.getSolverUsername().equals("ADMIN")) {
                    System.out.print("the administration team");
                } else System.out.print(r.getSolverUsername());
                System.out.print(" regarding ");
                switch (r.getRequestType()) {
                    case ACTOR_ISSUE:
                        System.out.print("an issue with an actor (" + r.getTitleOrMovie() + ")\n");
                        break;
                    case MOVIE_ISSUE:
                        System.out.print("an issue with a production (" + r.getTitleOrMovie() + ")\n");
                        break;
                    case DELETE_ACCOUNT:
                        System.out.print("the deletion of an account\n");
                        break;
                    case OTHERS:
                        System.out.print("other reasons\n");
                        break;
                }
                System.out.println("Description: " + r.getDescription());
                System.out.println();
            }
            System.out.print("Choose which request to delete: ");
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                if (choice > 0 && choice < RequestsMadeByMe.size() + 1) {
                    if (currentUser instanceof Regular) {
                        ((Regular) currentUser).removeRequest(RequestsMadeByMe.get(choice - 1));
                    } else if (currentUser instanceof Contributor) {
                        ((Contributor) currentUser).removeRequest(RequestsMadeByMe.get(choice - 1));
                    }
                } else {
                    System.out.println("Please enter valid input");
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter valid input");
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
                scanner.nextLine();
            }
        } else {
            System.out.println("You do not have any pending requests");
            System.out.print("Press Enter to go back...");
            scanner.nextLine();
        }
    }

    public static List<Request> getMyRequests(User currentUser) {
        List<Request> list = new ArrayList<>();
        for (Request request : Admin.RequestsHolder.getTeamRequests()) {
            if (request.getCreatorUsername().equals(currentUser.getUsername())) list.add(request);
        }
        for (Admin admin : adminList) {
            for (Request request : admin.getOwnList()) {
                if (request.getCreatorUsername().equals(currentUser.getUsername())) list.add(request);
            }
        }
        for (Contributor contributor : contributorList) {
            for (Request request : contributor.getOwnList()) {
                if (request.getCreatorUsername().equals(currentUser.getUsername())) list.add(request);
            }
        }
        return list;
    }

    public static void addDeleteFavorites(User currentUser) {
        clearConsole();
        System.out.print("Favorite actors: ");
        for (Actor actor : currentUser.getFavoriteActors()) {
            System.out.print(actor.getActorName());
            if (!actor.equals(currentUser.getFavoriteActors().getLast())) {
                System.out.print(", ");
            }
        }
        System.out.println();
        System.out.print("Favorite productions: ");
        for (Production production : currentUser.getFavoriteProductions()) {
            System.out.print(production.getTitle());
            if (!production.equals(currentUser.getFavoriteProductions().getLast())) {
                System.out.print(", ");
            }
        }
        System.out.println();
        System.out.print("Enter the name of the actor or the production you want to add to/delete from your favorites list: ");
        Scanner scanner = new Scanner(System.in);
        try {
            String name = scanner.nextLine();
            boolean ok = false;
            for (Actor actor : actors) {
                if (actor.getActorName().equals(name)) {
                    ok = true;
                    if (currentUser.getFavoriteActors().contains(actor)) {
                        currentUser.getFavoriteActors().remove(actor);
                        System.out.println("Actor removed from the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    } else {
                        currentUser.getFavoriteActors().add(actor);
                        System.out.println("Actor added to the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    }
                }
            }
            for (Movie movie : movies) {
                if (movie.getTitle().equals(name)) {
                    ok = true;
                    if (currentUser.getFavoriteProductions().contains(movie)) {
                        currentUser.getFavoriteProductions().remove(movie);
                        System.out.println("Movie removed from the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    } else {
                        currentUser.getFavoriteProductions().add(movie);
                        System.out.println("Movie added to the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    }
                }
            }
            for (Series series1 : series) {
                if (series1.getTitle().equals(name)) {
                    ok = true;
                    if (currentUser.getFavoriteProductions().contains(series1)) {
                        currentUser.getFavoriteProductions().remove(series1);
                        System.out.println("Series removed from the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    } else {
                        currentUser.getFavoriteProductions().add(series1);
                        System.out.println("Series added to the list successfully.");
                        System.out.print("Press Enter to go back");
                        scanner.nextLine();
                    }
                }
            }
            if (!ok) {
                System.out.println("Actor/Production does not exist in the system");
                System.out.print("Press Enter to go back");
                scanner.nextLine();
            }
        } catch (NoSuchElementException e) {
            System.out.println("Please put valid input");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public static void viewNotifications(User currentUser) {
        clearConsole();
        System.out.println("NOTIFICATIONS");
        int notificationNumber = 0;
        if (!currentUser.getNotifications().isEmpty()) {
            for (String notification : currentUser.getNotifications()) {
                notificationNumber++;
                System.out.println(notificationNumber + ") " + notification);
            }
        } else {
            System.out.println("No new notifications\n");
        }
    }

    public static void findAMS(User currentUser) {
        clearConsole();
        Scanner scanner = new Scanner(System.in);
        System.out.print(" Please enter the actor's name, the production's title or 'back' to go back: ");
        try {
            String name = scanner.nextLine();
            boolean ok = false;
            if (name.equals("back")) {
                ok = true;
            }
            for (Actor actor : actors) {
                if (name.equals(actor.getActorName())) {
                    clearConsole();
                    actor.displayInfo();
                    if (currentUser.getFavoriteActors().contains(actor)) {
                        System.out.println("Favorited: Yes\n");
                    } else {
                        System.out.println("Favorited: No\n");
                    }
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                for (Movie movie : movies) {
                    if (name.equals(movie.getTitle())) {
                        clearConsole();
                        movie.displayInfo();
                        System.out.println();
                        System.out.println("Ratings:\n");
                        movie.displayRatings();
                        if (currentUser.getFavoriteProductions().contains(movie)) {
                            System.out.println("Favorited: Yes\n");
                        } else {
                            System.out.println("Favorited: No\n");
                        }
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    for (Series series1 : series) {
                        if (name.equals(series1.getTitle())) {
                            clearConsole();
                            series1.displayInfo();
                            System.out.println();
                            System.out.println("Ratings:\n");
                            series1.displayRatings();
                            if (currentUser.getFavoriteProductions().contains(series1)) {
                                System.out.println("Favorited: Yes\n");
                            } else {
                                System.out.println("Favorited: No\n");
                            }
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        System.out.print("Actor or Production could not be found. Press Enter to try again.");
                        scanner.nextLine();
                        findAMS(currentUser);
                    }
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Please enter a valid name or title");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            findAMS(currentUser);
        }
    }

    //GUI
    private static void openWithGUI() {
        JFrame frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 280);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(247, 246, 242));
        frame.add(panel);
        placeComponents(panel, frame);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel, JFrame frame) {
        panel.setLayout(null);

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164), 20, true));

        JLabel titleLabel = new JLabel("IMDb");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(81, 191, 164));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setVerticalAlignment(JLabel.CENTER);

        titleLabel.setBounds(80, 40, 140, 60);
        titlePanel.setBounds(70, 30, 160, 80);

        panel.add(titleLabel);
        panel.add(titlePanel);

        JTextField emailField = new JTextField("email");
        emailField.setBounds(70, 125, 160, 25);
        emailField.setBackground(new Color(235, 230, 226));
        emailField.setForeground(Color.GRAY);

        emailField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (emailField.getText().equals("email")) {
                    emailField.setText("");
                    emailField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (emailField.getText().isEmpty()) {
                    emailField.setForeground(Color.GRAY);
                    emailField.setText("email");
                }
            }
        });

        panel.add(emailField);

        JPasswordField passwordField = new JPasswordField("Password");
        passwordField.setBounds(70, 160, 160, 25);
        passwordField.setBackground(new Color(235, 230, 226));
        passwordField.setForeground(Color.GRAY);
        passwordField.setEchoChar((char) 0);
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals("Password")) {
                    passwordField.setText("");
                    passwordField.setEchoChar('*');
                    passwordField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).isEmpty()) {
                    passwordField.setForeground(Color.GRAY);
                    passwordField.setEchoChar((char) 0);
                    passwordField.setText("Password");
                }
            }
        });

        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(50, 195, 80, 25);
        loginButton.setBackground(new Color(81, 191, 164));
        loginButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        loginButton.setForeground(Color.WHITE);
        panel.add(loginButton);

        JButton cancelButton = new JButton("Exit");
        cancelButton.setBounds(170, 195, 80, 25);
        cancelButton.setBackground(new Color(247, 246, 242));
        cancelButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        cancelButton.setForeground(new Color(81, 137, 124));
        panel.add(cancelButton);

        loginButton.addActionListener(e -> handleLogin(emailField.getText(), String.valueOf(passwordField.getPassword()), frame));
        cancelButton.addActionListener(e -> System.exit(0));
    }

    private static void handleLogin(String email, String password, JFrame frame) {
        User auxiliaryUser = GUIlogIn(email, password);
        if (auxiliaryUser != null) {
            JOptionPane.showMessageDialog(null, "Login successful!");
            // Proceed with the application logic for the logged-in user
            frame.dispose();
            mainMenu(auxiliaryUser);
        } else {
            JOptionPane.showMessageDialog(null, "Wrong credentials! Try again.");
        }
    }

    private static User GUIlogIn(String email, String password) {

        User auxiliaryUser = null;
        for (User user : regularList) {
            if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                auxiliaryUser = user;
            }
        }
        for (User user : contributorList) {
            if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                auxiliaryUser = user;
            }
        }
        for (User user : adminList) {
            if (user.getInformation().logIn(User.Information.credentialBuilder().email(email).password(password).build())) {
                auxiliaryUser = user;
            }
        }
        return  auxiliaryUser;
    }
    public static void mainMenu(User currentUser){
        JFrame frame = new JFrame("IMDb");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(247, 246, 242));
        frame.add(panel);
        mainMenuComponents(panel, frame, currentUser);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
    public static void updateUpperBorder(JPanel upperBorder, User currentUser) {
        upperBorder.removeAll();

        upperBorder.setBackground(new Color(81, 191, 164));
        upperBorder.setBounds(0, 0, 1400, 150);
        upperBorder.setLayout(null);

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5, true));
        titlePanel.setBackground(new Color(81, 191, 164));

        JLabel titleLabel = new JLabel("IMDb");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 80));
        titleLabel.setOpaque(false);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setVerticalAlignment(JLabel.CENTER);

        titleLabel.setBounds(40, 15, 200, 120);
        titlePanel.setBounds(20, 10, 240, 130);

        upperBorder.add(titleLabel);
        upperBorder.add(titlePanel);

        JLabel usernameLabel = new JLabel(currentUser.getUsername());
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        usernameLabel.setOpaque(false);
        usernameLabel.setBackground(new Color(81, 191, 164));
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setHorizontalAlignment(JLabel.CENTER);
        usernameLabel.setVerticalAlignment(JLabel.CENTER);
        usernameLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4, true));

        usernameLabel.setBounds(1080, 15, 280, 50);

        upperBorder.add(usernameLabel);

        JLabel experienceLabel = new JLabel("Experience: " + currentUser.getExp());
        experienceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        experienceLabel.setOpaque(false);
        experienceLabel.setBackground(new Color(81, 191, 164));
        experienceLabel.setForeground(Color.WHITE);
        experienceLabel.setHorizontalAlignment(JLabel.CENTER);
        experienceLabel.setVerticalAlignment(JLabel.CENTER);
        experienceLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4, true));

        experienceLabel.setBounds(1080, 85, 280, 50);

        upperBorder.add(experienceLabel);

        upperBorder.revalidate();
        upperBorder.repaint();
    }
    public static void mainMenuComponents(JPanel panel, JFrame frame, User currentUser){
        panel.setLayout(null);

        // UPPER PART OF MENU

        JPanel upperBorder = new JPanel();
        upperBorder.setBackground(new Color(81, 191, 164));
        upperBorder.setBounds(0, 0, 1400, 150);
        upperBorder.setLayout(null);

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5, true));
        titlePanel.setBackground(new Color(81, 191, 164));

        JLabel titleLabel = new JLabel("IMDb");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 80));
        titleLabel.setOpaque(false);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setVerticalAlignment(JLabel.CENTER);

        titleLabel.setBounds(40, 15, 200, 120);
        titlePanel.setBounds(20, 10, 240, 130);

        upperBorder.add(titleLabel);
        upperBorder.add(titlePanel);

        JLabel usernameLabel = new JLabel(currentUser.getUsername());
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        usernameLabel.setOpaque(false);
        usernameLabel.setBackground(new Color(81, 191, 164));
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setHorizontalAlignment(JLabel.CENTER);
        usernameLabel.setVerticalAlignment(JLabel.CENTER);
        usernameLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4, true));

        usernameLabel.setBounds(1080, 15, 280, 50);

        upperBorder.add(usernameLabel);

        JLabel experienceLabel = new JLabel("Experience: " + currentUser.getExp());
        experienceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        experienceLabel.setOpaque(false);
        experienceLabel.setBackground(new Color(81, 191, 164));
        experienceLabel.setForeground(Color.WHITE);
        experienceLabel.setHorizontalAlignment(JLabel.CENTER);
        experienceLabel.setVerticalAlignment(JLabel.CENTER);
        experienceLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4, true));

        experienceLabel.setBounds(1080, 85, 280, 50);

        upperBorder.add(experienceLabel);

        panel.add(upperBorder);

        updateUpperBorder(upperBorder, currentUser);

        //MAIN PART (BOTTOM LEFT)
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBackground(new Color(247, 246, 242));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 5));
        scrollPane.setBounds(0, 150, 1065, 615);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1));
        mainPanel.setBackground(new Color(247, 246, 242));
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 5));
        mainPanel.setBounds(0, 150, 1065, 615);

        JLabel welcomeBackLabel = new JLabel("Welcome back!");
        welcomeBackLabel.setFont(new Font("Arial", Font.BOLD, 40));
        welcomeBackLabel.setOpaque(false);
        welcomeBackLabel.setForeground(Color.BLACK);
        welcomeBackLabel.setHorizontalAlignment(JLabel.CENTER);
        welcomeBackLabel.setVerticalAlignment(JLabel.CENTER);
        welcomeBackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

       // mainPanel.add(welcomeBackLabel);

        JPanel notifPanel = new JPanel();
        notifPanel.setLayout(new GridLayout(5, 5));
        notifPanel.setBackground(new Color(247, 246, 242));


        JLabel log1Label = new JLabel("                         You have " + currentUser.getNotifications().size() + " new notifications.");
        log1Label.setFont(new Font("Arial", Font.BOLD, 15));
        log1Label.setOpaque(false);
        log1Label.setForeground(Color.BLACK);
        log1Label.setHorizontalAlignment(JLabel.LEFT);
        log1Label.setVerticalAlignment(JLabel.CENTER);
        log1Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        notifPanel.add(log1Label);
       // mainPanel.add(log1Label);

        JLabel log3Label;
        if (currentUser instanceof Staff){
            String message = "                         You have ";
            if (currentUser instanceof Admin) message += ((Admin) currentUser).getOwnList().size() + Admin.RequestsHolder.getTeamRequests().size();
            else message += ((Staff) currentUser).getOwnList().size();
            message += " new requests";

            log3Label = new JLabel(message);
            log3Label.setFont(new Font("Arial", Font.BOLD, 15));
            log3Label.setOpaque(false);
            log3Label.setForeground(Color.BLACK);
            log3Label.setHorizontalAlignment(JLabel.LEFT);
            log3Label.setVerticalAlignment(JLabel.CENTER);
            log3Label.setAlignmentX(Component.CENTER_ALIGNMENT);


            log3Label.setBounds(100 , 150, 300, 20);

           // mainPanel.add(log3Label);
            notifPanel.add(log3Label);
        }

        JLabel log2Label = new JLabel("                         Please choose what to do next.");
        log2Label.setFont(new Font("Arial", Font.BOLD, 15));
        log2Label.setOpaque(false);
        log2Label.setForeground(Color.BLACK);
        log2Label.setHorizontalAlignment(JLabel.LEFT);
        log2Label.setVerticalAlignment(JLabel.CENTER);
        log2Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        notifPanel.add(log2Label);

        mainPanel.add(welcomeBackLabel);
        mainPanel.add(notifPanel);

        scrollPane.setViewportView(mainPanel);

        // BUTTON PART (BOTTOM RIGHT)
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(null);
        buttonsPanel.setBackground(new Color(247, 246, 242));
        buttonsPanel.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 5));
        buttonsPanel.setBounds(1065, 150, 335, 615);
        panel.add(buttonsPanel);

        JButton productionsDetails = new JButton("View productions details");
        productionsDetails.setBounds(50, 50, 235, 40);
        productionsDetails.setBackground(new Color(81, 191, 164));
        productionsDetails.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        productionsDetails.setForeground(Color.WHITE);
        productionsDetails.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(productionsDetails);
        productionsDetails.addActionListener(e ->viewProductionDetailsGUI(panel, frame, mainPanel, currentUser));

        JButton actorsDetails = new JButton("View actors details");
        actorsDetails.setBounds(50, 100, 235, 40);
        actorsDetails.setBackground(new Color(81, 191, 164));
        actorsDetails.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        actorsDetails.setForeground(Color.WHITE);
        actorsDetails.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(actorsDetails);
        actorsDetails.addActionListener(e ->viewActorsDetailsGUI(panel, frame, mainPanel, currentUser));

        JButton notificationsDetails = new JButton("View notifications");
        notificationsDetails.setBounds(50, 150, 235, 40);
        notificationsDetails.setBackground(new Color(81, 191, 164));
        notificationsDetails.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        notificationsDetails.setForeground(Color.WHITE);
        notificationsDetails.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(notificationsDetails);
        notificationsDetails.addActionListener(e -> viewNotificationsGUI(panel, frame, mainPanel, currentUser));

        JButton searchProductionActor = new JButton("Search for a production/actor");
        searchProductionActor.setBounds(50, 200, 235, 40);
        searchProductionActor.setBackground(new Color(81, 191, 164));
        searchProductionActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        searchProductionActor.setForeground(Color.WHITE);
        searchProductionActor.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(searchProductionActor);
        searchProductionActor.addActionListener(e -> searchProductionActorGUI(upperBorder, panel, frame, mainPanel, currentUser));

        JButton favoriteProductions = new JButton("See favorites");
        favoriteProductions.setBounds(50, 250, 235, 40);
        favoriteProductions.setBackground(new Color(81, 191, 164));
        favoriteProductions.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        favoriteProductions.setForeground(Color.WHITE);
        favoriteProductions.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(favoriteProductions);
        favoriteProductions.addActionListener(e -> viewFavoritesGUI(panel, frame, mainPanel, currentUser));

        if(currentUser instanceof Admin){
            JButton addAUser = new JButton("Add/Delete user");
            addAUser.setBounds(50, 300, 235, 40);
            addAUser.setBackground(new Color(81, 191, 164));
            addAUser.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            addAUser.setForeground(Color.WHITE);
            addAUser.setFont(new Font("Arial", Font.BOLD, 15));
            addAUser.addActionListener(e -> addDeleteUserGUI(panel, frame, mainPanel, (Admin) currentUser));
            buttonsPanel.add(addAUser);
        }
        else {
            JButton makeARequest = new JButton("Make a request");
            makeARequest.setBounds(50, 300, 235, 40);
            makeARequest.setBackground(new Color(81, 191, 164));
            makeARequest.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            makeARequest.setForeground(Color.WHITE);
            makeARequest.setFont(new Font("Arial", Font.BOLD, 15));
            buttonsPanel.add(makeARequest);
            makeARequest.addActionListener(e -> makeARequest(panel, frame, mainPanel, currentUser));
        }

        if (currentUser instanceof Staff){
            JButton addActor = new JButton("Add Actor/Production");
            addActor.setBounds(50, 350, 235, 40);
            addActor.setBackground(new Color(81, 191, 164));
            addActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            addActor.setForeground(Color.WHITE);
            addActor.setFont(new Font("Arial", Font.BOLD, 15));
            addActor.addActionListener(e -> addActorProductionGUI(upperBorder, panel, frame, mainPanel,(Staff) currentUser));
            buttonsPanel.add(addActor);

            JButton removeActor = new JButton("Remove Actor/Production");
            removeActor.setBounds(50, 400, 235, 40);
            removeActor.setBackground(new Color(81, 191, 164));
            removeActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            removeActor.setForeground(Color.WHITE);
            removeActor.setFont(new Font("Arial", Font.BOLD, 15));
            removeActor.addActionListener(e -> removeActorProduction(panel, frame, mainPanel, (Staff) currentUser));
            buttonsPanel.add(removeActor);

            JButton solveARequest = new JButton("Solve a request");
            solveARequest.setBounds(50, 450, 235, 40);
            solveARequest.setBackground(new Color(81, 191, 164));
            solveARequest.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            solveARequest.setForeground(Color.WHITE);
            solveARequest.setFont(new Font("Arial", Font.BOLD, 15));
            solveARequest.addActionListener(e -> solveARequestGUI(panel, frame, mainPanel,(Staff) currentUser));
            buttonsPanel.add(solveARequest);
        }

        JButton logOutButton = new JButton("Log out");
        logOutButton.setBounds(100, 550, 135, 40);
        logOutButton.setBackground(new Color(247, 246, 242));
        logOutButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        logOutButton.setForeground(new Color(81, 137, 124));
        logOutButton.setFont(new Font("Arial", Font.BOLD, 15));
        buttonsPanel.add(logOutButton);
        logOutButton.addActionListener(e -> {
            frame.dispose();
            openWithGUI();
        });
    }
    private static JLabel makeLabel(String msg,int font, int textSize, int r, int g, int b){
        JLabel newLable = new JLabel(msg);
        newLable.setFont(new Font("Arial", font, textSize));
        newLable.setOpaque(false);
        newLable.setForeground(Color.BLACK);
        newLable.setHorizontalAlignment(JLabel.LEFT);
        newLable.setVerticalAlignment(JLabel.CENTER);
        return newLable;
    }
    public static void viewProductionDetailsGUI(JPanel jpanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        ButtonGroup buttonGroup = new ButtonGroup();

        JRadioButton movieButton = new JRadioButton("Movies");
        JRadioButton seriesButton = new JRadioButton("Series");

        buttonGroup.add(movieButton);
        buttonGroup.add(seriesButton);

        JLabel choice = makeLabel("Show: ", Font.BOLD,15, 81, 191, 164);

        JLabel sortChoice = makeLabel("Filter by: ",Font.BOLD, 15, 81, 191, 164);
        JComboBox<Genre> sortChoices = new JComboBox<>(Genre.values());
        sortChoices.addItem(null);
        sortChoices.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "No Filter", index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
        });
        sortChoices.setSelectedItem(null);
        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBoxPanel.setBackground(new Color(247, 246, 242));

        JButton showButton = new JButton("Show");
        showButton.setBackground(new Color(81, 191, 164));
        showButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        showButton.setForeground(Color.WHITE);
        showButton.setFont(new Font("Arial", Font.BOLD, 15));
        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printMovieList(jpanel, frame, panel,(Genre) sortChoices.getSelectedItem(), movieButton.isSelected(), seriesButton.isSelected(), currentUser);
            }
        });

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jpanel.removeAll();
                mainMenuComponents(jpanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        comboBoxPanel.add(Box.createHorizontalStrut(80));
        comboBoxPanel.add(choice);
        comboBoxPanel.add(movieButton);
        comboBoxPanel.add(seriesButton);
        comboBoxPanel.add(sortChoice);
        comboBoxPanel.add(sortChoices);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(showButton);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(goBack);
        buttonPanel.add(comboBoxPanel);

        panel.add(buttonPanel);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    private static void printMovieList(JPanel jpanel, JFrame frame, JPanel panel, Genre selectedGenre, boolean isMovieSelected, boolean isSeriesSelected, User currentUser){
        panel.removeAll();

        ButtonGroup buttonGroup = new ButtonGroup();

        JRadioButton movieButton = new JRadioButton("Movies");
        JRadioButton seriesButton = new JRadioButton("Series");

        buttonGroup.add(movieButton);
        buttonGroup.add(seriesButton);

        JLabel choice = makeLabel("Show: ", Font.BOLD,15, 81, 191, 164);

        JLabel sortChoice = makeLabel("Filter by: ",Font.BOLD, 15, 81, 191, 164);
        JComboBox<Genre> sortChoices = new JComboBox<>(Genre.values());
        sortChoices.addItem(null);
        sortChoices.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "No Filter", index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
        });
        sortChoices.setSelectedItem(selectedGenre);
        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBoxPanel.setBackground(new Color(247, 246, 242));

        JButton showButton = new JButton("Show");
        showButton.setBackground(new Color(81, 191, 164));
        showButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        showButton.setForeground(Color.WHITE);
        showButton.setFont(new Font("Arial", Font.BOLD, 15));
        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printMovieList(jpanel, frame, panel, (Genre) sortChoices.getSelectedItem(), movieButton.isSelected(), seriesButton.isSelected(), currentUser);
            }
        });

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jpanel.removeAll();
                mainMenuComponents(jpanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        comboBoxPanel.add(Box.createHorizontalStrut(80));
        comboBoxPanel.add(choice);
        comboBoxPanel.add(movieButton);
        comboBoxPanel.add(seriesButton);
        comboBoxPanel.add(sortChoice);
        comboBoxPanel.add(sortChoices);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(showButton);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(goBack);
        buttonPanel.add(comboBoxPanel);

        panel.add(buttonPanel);

        if (isMovieSelected) {
            for (Movie movie : movies) {
                if (movie.getGenres().contains(selectedGenre) || selectedGenre == null) {
                    JLabel title = makeLabel(movie.getTitle() + " (" + movie.getReleaseYear() + ")", Font.BOLD, 15, 81, 191, 164);
                    title.setAlignmentX(Component.LEFT_ALIGNMENT);
                    title.setForeground(new Color(81, 137, 124));

                    JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    titlePanel.setBackground(new Color(247, 246, 242));
                    titlePanel.add(title);

                    panel.add(titlePanel);

                    StringBuilder genreString = new StringBuilder("Genres: ");
                    for (Genre genre : movie.getGenres()) {
                        genreString.append(genre.toString());
                        if (!genre.equals(movie.getGenres().getLast())) {
                            genreString.append(", ");
                        }
                    }
                    JLabel genres = makeLabel(genreString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    genres.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel genrePanel = new JPanel(new BorderLayout());
                    genrePanel.setBackground(new Color(247, 246, 242));
                    genrePanel.add(genres);

                    panel.add(genrePanel);

                    JLabel rating = makeLabel("Average Rating: " + movie.getAverageRating().toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    rating.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel ratingPanel = new JPanel(new BorderLayout());
                    ratingPanel.setBackground(new Color(247, 246, 242));
                    ratingPanel.add(rating);

                    panel.add(ratingPanel);

                    JLabel length = makeLabel("Length: " + movie.getMovieLength(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    length.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel lengthPanel = new JPanel(new BorderLayout());
                    lengthPanel.setBackground(new Color(247, 246, 242));
                    lengthPanel.add(length);

                    panel.add(lengthPanel);

                    JTextArea plot = new JTextArea("Plot: " + movie.getPlot());
                    plot.setBackground(new Color(247, 246, 242));
                    plot.setForeground(Color.BLACK);
                    plot.setFont(new Font("Arial", Font.PLAIN, 15));
                    plot.setEditable(false);
                    plot.setWrapStyleWord(true);
                    plot.setLineWrap(true);
                    plot.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel plotPanel= new JPanel(new BorderLayout());
                    plotPanel.setBackground(new Color(247, 246, 242));
                    plotPanel.add(plot);

                    panel.add(plotPanel);

                    StringBuilder directorString = new StringBuilder("Directors: ");
                    for (String director : movie.getDirectors()) {
                        directorString.append(director);
                        if (!director.equals(movie.getDirectors().getLast())) {
                            directorString.append(", ");
                        }
                    }
                    JLabel directors = makeLabel(directorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    directors.setAlignmentX(Component.LEFT_ALIGNMENT);
                    JPanel directorsPanel = new JPanel(new BorderLayout());
                    directorsPanel.setBackground(new Color(247, 246, 242));
                    directorsPanel.add(directors);

                    panel.add(directorsPanel);

                    StringBuilder actorString = new StringBuilder("Actors: ");
                    for (String actor : movie.getActors()) {
                        actorString.append(actor);
                        if (!actor.equals(movie.getActors().getLast())) {
                            actorString.append(", ");
                        }
                    }
                    JLabel actors = makeLabel(actorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    actors.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel actorsPanel = new JPanel(new BorderLayout());
                    actorsPanel.setBackground(new Color(247, 246, 242));
                    actorsPanel.add(actors);
                    panel.add(actorsPanel);

                    JToggleButton favoritesButton = new JToggleButton();
                    if (currentUser.getFavoriteProductions().contains(movie)){
                        favoritesButton.setSelected(true);
                        favoritesButton.setText("  Remove from Favorites  ");
                        favoritesButton.setBackground(new Color(247, 246, 242));
                        favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                        favoritesButton.setForeground(new Color(81, 137, 124));
                        favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
                    }
                    else {
                        favoritesButton.setSelected(false);
                        favoritesButton.setText("        Add to Favorites        ");
                        favoritesButton.setBackground(new Color(81, 191, 164));
                        favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                        favoritesButton.setForeground(Color.WHITE);
                        favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
                    }
                    favoritesButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            AbstractButton abstractButton = (AbstractButton) e.getSource();
                            boolean selected = abstractButton.getModel().isSelected();

                            if (selected) {
                                abstractButton.setText("  Remove from Favorites  ");
                                abstractButton.setBackground(new Color(247, 246, 242));
                                abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                                abstractButton.setForeground(new Color(81, 137, 124));
                                abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                                currentUser.addFavoriteProduction(movie);
                            } else {
                                abstractButton.setText("        Add to Favorites        ");
                                abstractButton.setBackground(new Color(81, 191, 164));
                                abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                                abstractButton.setForeground(Color.WHITE);
                                abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                                currentUser.removeFavoriteProduction(movie);
                            }
                        }
                    });
                    JPanel toggleButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    toggleButtonPanel.setBackground(new Color(247, 246, 242));
                    toggleButtonPanel.add(favoritesButton);

                    panel.add(toggleButtonPanel);

                    panel.add(new Label());
                }
            }
        }
        else if(isSeriesSelected){
            for (Series currentSeries : series) {
                if (currentSeries.getGenres().contains(selectedGenre) || selectedGenre == null) {
                    JLabel title = makeLabel(currentSeries.getTitle() + " (" + currentSeries.getReleaseYear() + ")", Font.BOLD, 15, 81, 191, 164);
                    title.setAlignmentX(Component.LEFT_ALIGNMENT);
                    title.setForeground(new Color(81, 137, 124));

                    JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    titlePanel.setBackground(new Color(247, 246, 242));
                    titlePanel.add(title);

                    panel.add(titlePanel);

                    StringBuilder genreString = new StringBuilder("Genres: ");
                    for (Genre genre : currentSeries.getGenres()) {
                        genreString.append(genre.toString());
                        if (!genre.equals(currentSeries.getGenres().getLast())) {
                            genreString.append(", ");
                        }
                    }
                    JLabel genres = makeLabel(genreString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    genres.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel genrePanel = new JPanel(new BorderLayout());
                    genrePanel.setBackground(new Color(247, 246, 242));
                    genrePanel.add(genres);

                    panel.add(genrePanel);

                    JLabel rating = makeLabel("Average Rating: " + currentSeries.getAverageRating().toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    rating.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel ratingPanel = new JPanel(new BorderLayout());
                    ratingPanel.setBackground(new Color(247, 246, 242));
                    ratingPanel.add(rating, BorderLayout.CENTER);

                    panel.add(ratingPanel);

                    JLabel length = makeLabel("Number of seasons: " + currentSeries.getSeasonsNumber(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    length.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel lengthLabel = new JPanel(new BorderLayout());
                    lengthLabel.setBackground(new Color(247, 246, 242));
                    lengthLabel.add(length, BorderLayout.CENTER);

                    panel.add(lengthLabel);

                    JTextArea plot = new JTextArea("Plot: " + currentSeries.getPlot());
                    plot.setBackground(Color.BLACK);
                    plot.setForeground(Color.BLACK);
                    plot.setFont(new Font("Arial", Font.PLAIN, 15));
                    plot.setEditable(false);
                    plot.setWrapStyleWord(true);
                    plot.setLineWrap(true);
                    plot.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel plotPanel = new JPanel(new BorderLayout());
                    plotPanel.setBackground(new Color(247, 246, 242));
                    plotPanel.add(plot, BorderLayout.CENTER);

                    panel.add(plotPanel);

                    StringBuilder directorString = new StringBuilder("Directors: ");
                    for (String director : currentSeries.getDirectors()) {
                        directorString.append(director);
                        if (!director.equals(currentSeries.getDirectors().getLast())) {
                            directorString.append(", ");
                        }
                    }
                    JLabel directors = makeLabel(directorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    directors.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel directorsPanel = new JPanel(new BorderLayout());
                    directorsPanel.setBackground(new Color(247, 246, 242));
                    directorsPanel.add(directors, BorderLayout.CENTER);

                    panel.add(directorsPanel);

                    StringBuilder actorString = new StringBuilder("Actors: ");
                    for (String actor : currentSeries.getActors()) {
                        actorString.append(actor);
                        if (!actor.equals(currentSeries.getActors().getLast())) {
                            actorString.append(", ");
                        }
                    }
                    JLabel actors = makeLabel(actorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                    actors.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel actorsPanel = new JPanel(new BorderLayout());
                    actorsPanel.setBackground(new Color(247, 246, 242));
                    actorsPanel.add(actors, BorderLayout.CENTER);

                    panel.add(actorsPanel);

                    JToggleButton favoritesButton = new JToggleButton();
                    if (currentUser.getFavoriteProductions().contains(currentSeries)){
                        favoritesButton.setSelected(true);
                        favoritesButton.setText("  Remove from Favorites  ");
                        favoritesButton.setBackground(new Color(247, 246, 242));
                        favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                        favoritesButton.setForeground(new Color(81, 137, 124));
                        favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
                    }
                    else {
                        favoritesButton.setSelected(false);
                        favoritesButton.setText("        Add to Favorites        ");
                        favoritesButton.setBackground(new Color(81, 191, 164));
                        favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                        favoritesButton.setForeground(Color.WHITE);
                        favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
                    }
                    favoritesButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            AbstractButton abstractButton = (AbstractButton) e.getSource();
                            boolean selected = abstractButton.getModel().isSelected();

                            if (selected) {
                                abstractButton.setText("  Remove from Favorites  ");
                                abstractButton.setBackground(new Color(247, 246, 242));
                                abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                                abstractButton.setForeground(new Color(81, 137, 124));
                                abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                                currentUser.addFavoriteProduction(currentSeries);
                            } else {
                                abstractButton.setText("        Add to Favorites        ");
                                abstractButton.setBackground(new Color(81, 191, 164));
                                abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                                abstractButton.setForeground(Color.WHITE);
                                abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                                currentUser.removeFavoriteProduction(currentSeries);
                            }
                        }
                    });

                    JPanel toggleButtonLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    toggleButtonLabel.setBackground(new Color(247, 246, 242));
                    toggleButtonLabel.add(favoritesButton);

                    panel.add(toggleButtonLabel);

                    panel.add(new Label());

                    for (Map.Entry<String, List<Episode>> entry : currentSeries.getSeasons().entrySet()) {
                        String seasonNumber = entry.getKey();
                        List<Episode> episodes = entry.getValue();

                        JLabel season = makeLabel(seasonNumber + ":", Font.TRUETYPE_FONT, 15, 81, 191, 164);
                        season.setAlignmentX(Component.LEFT_ALIGNMENT);

                        JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        seasonsPanel.setBackground(new Color(247, 246, 242));
                        seasonsPanel.add(season);

                        panel.add(seasonsPanel);
                        int i = 0;
                        for (Episode episode : episodes) {
                            i++;
                            JLabel ep = makeLabel("    Episode " + i + " - " + episode.getEpisodeName(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                            ep.setAlignmentX(Component.LEFT_ALIGNMENT);

                            JPanel episodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                            episodePanel.setBackground(new Color(247, 246, 242));
                            episodePanel.add(ep);

                            panel.add(episodePanel);
                        }
                    }

                    panel.add(new Label());
                }
            }
        }

        panel.revalidate();
        panel.repaint();
    }
    public static void viewActorsDetailsGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JLabel sortChoice = makeLabel("Sort by: ",Font.BOLD, 15, 81, 191, 164);
        JComboBox<String> sortChoices = new JComboBox<String>();
        sortChoices.addItem(null);
        sortChoices.addItem("Name");
        sortChoices.addItem("Number of performances");
        sortChoices.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "No Filter", index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
        });
        sortChoices.setSelectedItem(null);

        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBoxPanel.setBackground(new Color(247, 246, 242));

        JButton showButton = new JButton("Show");
        showButton.setBackground(new Color(81, 191, 164));
        showButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        showButton.setForeground(Color.WHITE);
        showButton.setFont(new Font("Arial", Font.BOLD, 15));
        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printActorsList(panel,(String) sortChoices.getSelectedItem(), jPanel, frame, currentUser, scrollPane);
            }
        });

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        comboBoxPanel.add(Box.createHorizontalStrut(80));
        comboBoxPanel.add(sortChoice);
        comboBoxPanel.add(sortChoices);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(showButton);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(goBack);
        buttonPanel.add(comboBoxPanel);

        panel.add(buttonPanel);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public static List<Actor> sortActorList(List<Actor> actorList, String sortingChoice){
        if (sortingChoice == null){
            return actors;
        }
        else if (sortingChoice.equals("Name")){
            actorList.sort(Comparator.comparing(Actor::getActorName));
            return actorList;
        }
        else if (sortingChoice.equals("Number of performances")){
            actorList.sort(Comparator.comparingInt(actor -> actor.getPerformances().size()));
            return actorList.reversed();
        }
        else return actors;
    }
    public static void printActorsList(JPanel panel, String sortingChoice, JPanel jPanel, JFrame frame, User currentUser, JPanel scrollPane){
        panel.removeAll();
        List<Actor> actorList = actors;
        JLabel sortChoice = makeLabel("Sort by: ",Font.BOLD, 15, 81, 191, 164);
        JComboBox<String> sortChoices = new JComboBox<>();
        sortChoices.addItem(null);
        sortChoices.addItem("Name");
        sortChoices.addItem("Number of performances");
        sortChoices.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "No Filter", index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
        });
        sortChoices.setSelectedItem(sortingChoice);

        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBoxPanel.setBackground(new Color(247, 246, 242));

        JButton showButton = new JButton("Show");
        showButton.setBackground(new Color(81, 191, 164));
        showButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        showButton.setForeground(Color.WHITE);
        showButton.setFont(new Font("Arial", Font.BOLD, 15));
        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printActorsList(panel, (String) sortChoices.getSelectedItem(), jPanel, frame, currentUser, scrollPane);
            }
        });

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        comboBoxPanel.add(Box.createHorizontalStrut(80));
        comboBoxPanel.add(sortChoice);
        comboBoxPanel.add(sortChoices);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(showButton);
        comboBoxPanel.add(Box.createHorizontalStrut(50));
        comboBoxPanel.add(goBack);
        buttonPanel.add(comboBoxPanel);

        panel.add(buttonPanel);

        actorList = sortActorList(actorList, sortingChoice);

        for (Actor actor : actorList) {
            JLabel name = makeLabel(actor.getActorName().toUpperCase(), Font.BOLD, 15, 81, 191, 164);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            name.setForeground(new Color(81, 137, 124));

            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            namePanel.setBackground(new Color(247, 246, 242));
            namePanel.add(name);

            panel.add(namePanel);

            JTextArea plot = new JTextArea("Biography: " + actor.getBiography());
            plot.setBackground(new Color(247, 246, 242));
            plot.setForeground(Color.BLACK);
            plot.setFont(new Font("Arial", Font.PLAIN, 15));
            plot.setEditable(false);
            plot.setWrapStyleWord(true);
            plot.setLineWrap(true);
            plot.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel plotPanel = new JPanel(new BorderLayout());
            plotPanel.setBackground(new Color(247, 246, 242));
            plotPanel.add(plot, BorderLayout.CENTER);

            panel.add(plotPanel);

            int currentEntry = 0;
            int entryCount = actor.getPerformances().size();
            StringBuilder performances = new StringBuilder("Known for: ");
            for (Map.Entry<String, String> entry : actor.getPerformances().entrySet()){
                performances.append("\"").append(entry.getKey()).append("\"");
                if (++currentEntry < entryCount) {
                    performances.append(", ");
                }
            }

            JTextArea performance = new JTextArea(performances.toString());
            performance.setBackground(new Color(247, 246, 242));
            performance.setForeground(Color.BLACK);
            performance.setFont(new Font("Arial", Font.BOLD, 15));
            performance.setEditable(false);
            performance.setWrapStyleWord(true);
            performance.setLineWrap(true);
            performance.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel performancePanel = new JPanel(new BorderLayout());
            performancePanel.setBackground(new Color(247, 246, 242));
            performancePanel.add(performance, BorderLayout.CENTER);

            panel.add(performancePanel);

            JToggleButton favoritesButton = new JToggleButton();
            if (currentUser.getFavoriteActors().contains(actor)){
                favoritesButton.setSelected(true);
                favoritesButton.setText("  Remove from Favorites  ");
                favoritesButton.setBackground(new Color(247, 246, 242));
                favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                favoritesButton.setForeground(new Color(81, 137, 124));
                favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
            }
            else {
                favoritesButton.setSelected(false);
                favoritesButton.setText("        Add to Favorites        ");
                favoritesButton.setBackground(new Color(81, 191, 164));
                favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                favoritesButton.setForeground(Color.WHITE);
                favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
            }
            favoritesButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AbstractButton abstractButton = (AbstractButton) e.getSource();
                    boolean selected = abstractButton.getModel().isSelected();

                    if (selected) {
                        abstractButton.setText("  Remove from Favorites  ");
                        abstractButton.setBackground(new Color(247, 246, 242));
                        abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                        abstractButton.setForeground(new Color(81, 137, 124));
                        abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                        currentUser.addFavoriteActor(actor);
                    } else {
                        abstractButton.setText("        Add to Favorites        ");
                        abstractButton.setBackground(new Color(81, 191, 164));
                        abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                        abstractButton.setForeground(Color.WHITE);
                        abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                        currentUser.addFavoriteActor(actor);
                    }
                }
            });
            JPanel toggleButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toggleButtonPanel.setBackground(new Color(247, 246, 242));
            toggleButtonPanel.add(favoritesButton);

            panel.add(toggleButtonPanel);

            panel.add(new Label());
        }

        panel.revalidate();
        panel.repaint();
    }

    public static void viewNotificationsGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(900));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        panel.add(upperButtons);


        if (!currentUser.getNotifications().isEmpty()) {
            for (String notification : currentUser.getNotifications()) {
                JPanel notif = new JPanel(new FlowLayout((FlowLayout.LEFT)));
                notif.setBackground(new Color(247, 246, 242));
                notif.add(makeLabel(notification, Font.BOLD, 15, 81, 191, 164));
                panel.add(notif);
            }
        }
        else {
            JPanel notif = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            notif.setBackground(new Color(247, 246, 242));
            notif.add(makeLabel("No new notifications", Font.BOLD, 15, 81, 191, 164));
            panel.add(notif);
        }
        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public static void viewFavoritesGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(900));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        panel.add(upperButtons);

        StringBuilder favActors = new StringBuilder("Favorite Actors: ");

        if (!currentUser.getFavoriteActors().isEmpty()) {
            for (Actor actor : currentUser.getFavoriteActors()) {


                favActors.append(actor.getActorName());
                if (!actor.equals(currentUser.getFavoriteActors().getLast())){
                    favActors.append(", ");
                }
            }
        }
        else {
            favActors.append("No favorite actors");
        }

        JPanel favoriteActors = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        favoriteActors.setBackground(new Color(247, 246, 242));
        favoriteActors.add(makeLabel(favActors.toString(), Font.BOLD, 15, 81, 191, 164));
        panel.add(favoriteActors);

        panel.add(new Label());

        StringBuilder favProductions = new StringBuilder("Favorite Productions: ");

        if (!currentUser.getFavoriteProductions().isEmpty()) {
            for (Production production : currentUser.getFavoriteProductions()) {
                favProductions.append(production.getTitle());
                if (!production.equals(currentUser.getFavoriteProductions().getLast())){
                    favProductions.append(", ");
                }
            }
        }
        else {
            favProductions.append("No favorite productions");
        }

        JPanel favoriteProductions = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        favoriteProductions.setBackground(new Color(247, 246, 242));
        favoriteProductions.add(makeLabel(favProductions.toString(), Font.BOLD, 15, 81, 191, 164));
        panel.add(favoriteProductions);


        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public static void searchProductionActorGUI(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(900));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        panel.add(upperButtons);
        panel.add(new Label());
        panel.add(new Label());

        JTextField nameSearch = new JTextField("Enter the name/title...");
        nameSearch.setPreferredSize(new Dimension(500, 30));
        nameSearch.setBackground(new Color(235, 230, 226));
        nameSearch.setForeground(Color.GRAY);
        nameSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nameSearch.getText().equals("Enter the name/title...")) {
                    nameSearch.setText("");
                    nameSearch.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (nameSearch.getText().isEmpty()) {
                    nameSearch.setForeground(Color.GRAY);
                    nameSearch.setText("Enter the name/title...");
                }
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.setBackground(new Color(247, 246, 242));
        searchPanel.add(nameSearch);

        JButton loginButton = new JButton("Search");
        loginButton.setPreferredSize(new Dimension(100, 30));
        loginButton.setBackground(new Color(81, 191, 164));
        loginButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> handleSearch(upperBorder, jPanel, frame, scrollPane, currentUser, nameSearch.getText()));

        searchPanel.add(loginButton);

        panel.add(searchPanel);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    private static void handleSearch(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser, String searchText){
        Object found = null;
        for (Actor actor : actors){
            if (actor.getActorName().equals(searchText)){
                found = actor;
                break;
            }
        }
        for (Movie movie : movies){
            if (movie.getTitle().equals(searchText)){
                found = movie;
                break;
            }
        }
        for (Series series1 : series){
            if (series1.getTitle().equals(searchText)){
                found = series1;
                break;
            }
        }
        if (found != null) {
            if (found instanceof Actor) {
                JOptionPane.showMessageDialog(null, "Actor found!");
                actorFound(jPanel, frame, scrollPane, currentUser, (Actor) found);
            }
            else if(found instanceof Movie) {
                JOptionPane.showMessageDialog(null, "Movie found!");
                movieFound(upperBorder, jPanel, frame, scrollPane, currentUser, (Movie) found);
            }
            else {
                JOptionPane.showMessageDialog(null,"Series found!");
                seriesFound(upperBorder, jPanel, frame, scrollPane, currentUser, (Series) found);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Actor/Production not found!");
        }
    }
    private static void actorFound(JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser, Actor actor){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        buttonPanel.add(Box.createHorizontalStrut(900));
        buttonPanel.add(goBack);

        panel.add(buttonPanel);
        panel.add(new Label());

        JLabel name = makeLabel(actor.getActorName().toUpperCase(), Font.BOLD, 40, 81, 191, 164);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.setBackground(new Color(247, 246, 242));

        String imgPath = "utils/actors/" + actor.getActorName() + ".jpg";

        File imageFile = new File(imgPath);
        ImageIcon icon;
        if (imageFile.exists()) icon = new ImageIcon(imgPath);
        else icon = new ImageIcon("utils/actors/Default.jpg");

        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);

        ImageIcon resizedIcon = new ImageIcon(scaledImage);

        JLabel actorImage = new JLabel(resizedIcon);

        namePanel.add(actorImage);
        namePanel.add(name);

        if (currentUser instanceof Staff && ((Staff) currentUser).getActorsAdded().contains(actor)){
            JButton editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension( 80, 25));
            editButton.setBackground(new Color(81, 191, 164));
            editButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            editButton.setForeground(Color.WHITE);
            auxiliaryActor = actor;
            editButton.addActionListener(e -> updateActorGUI(jPanel, frame, scrollPane, (Staff) currentUser, actor));

            namePanel.add(editButton);
        }

        panel.add(namePanel);

        JTextArea plot = new JTextArea("Biography: " + actor.getBiography());
        plot.setBackground(new Color(247, 246, 242));
        plot.setForeground(Color.BLACK);
        plot.setFont(new Font("Arial", Font.PLAIN, 15));
        plot.setEditable(false);
        plot.setWrapStyleWord(true);
        plot.setLineWrap(true);
        plot.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel plotPanel = new JPanel(new BorderLayout());
        plotPanel.setBackground(new Color(247, 246, 242));
        plotPanel.add(plot, BorderLayout.CENTER);

        panel.add(plotPanel);

        int currentEntry = 0;
        int entryCount = actor.getPerformances().size();
        StringBuilder performances = new StringBuilder("Known for: ");
        for (Map.Entry<String, String> entry : actor.getPerformances().entrySet()){
            performances.append("\"").append(entry.getKey()).append("\"");
            if (++currentEntry < entryCount) {
                performances.append(", ");
            }
        }

        JTextArea performance = new JTextArea(performances.toString());
        performance.setBackground(new Color(247, 246, 242));
        performance.setForeground(Color.BLACK);
        performance.setFont(new Font("Arial", Font.BOLD, 15));
        performance.setEditable(false);
        performance.setWrapStyleWord(true);
        performance.setLineWrap(true);
        performance.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel performancePanel = new JPanel(new BorderLayout());
        performancePanel.setBackground(new Color(247, 246, 242));
        performancePanel.add(performance, BorderLayout.CENTER);

        panel.add(performancePanel);

        JToggleButton favoritesButton = new JToggleButton();
        if (currentUser.getFavoriteActors().contains(actor)){
            favoritesButton.setSelected(true);
            favoritesButton.setText("  Remove from Favorites  ");
            favoritesButton.setBackground(new Color(247, 246, 242));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
            favoritesButton.setForeground(new Color(81, 137, 124));
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        else {
            favoritesButton.setSelected(false);
            favoritesButton.setText("        Add to Favorites        ");
            favoritesButton.setBackground(new Color(81, 191, 164));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            favoritesButton.setForeground(Color.WHITE);
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        favoritesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                boolean selected = abstractButton.getModel().isSelected();

                if (selected) {
                    abstractButton.setText("  Remove from Favorites  ");
                    abstractButton.setBackground(new Color(247, 246, 242));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                    abstractButton.setForeground(new Color(81, 137, 124));
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.addFavoriteActor(actor);
                } else {
                    abstractButton.setText("        Add to Favorites        ");
                    abstractButton.setBackground(new Color(81, 191, 164));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                    abstractButton.setForeground(Color.WHITE);
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.addFavoriteActor(actor);
                }
            }
        });
        JPanel toggleButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toggleButtonPanel.setBackground(new Color(247, 246, 242));
        toggleButtonPanel.add(favoritesButton);

        panel.add(toggleButtonPanel);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    private static void movieFound(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser, Movie movie){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        buttonPanel.add(Box.createHorizontalStrut(900));
        buttonPanel.add(goBack);

        panel.add(buttonPanel);
        panel.add(new Label());

        JLabel title = makeLabel(movie.getTitle() + " (" + movie.getReleaseYear() + ")", Font.BOLD, 15, 81, 191, 164);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(new Color(247, 246, 242));
        titlePanel.add(title);

        if (currentUser instanceof Staff && ((Staff) currentUser).getProductionsAdded().contains(movie)){
            JButton editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension( 80, 25));
            editButton.setBackground(new Color(81, 191, 164));
            editButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            editButton.setForeground(Color.WHITE);
            auxiliaryMovie = movie;
            editButton.addActionListener(e -> updateMovieGUI(jPanel, frame, scrollPane, (Staff) currentUser, movie));

            titlePanel.add(editButton);
        }

        panel.add(titlePanel);

        StringBuilder genreString = new StringBuilder("Genres: ");
        for (Genre genre : movie.getGenres()) {
            genreString.append(genre.toString());
            if (!genre.equals(movie.getGenres().getLast())) {
                genreString.append(", ");
            }
        }
        JLabel genres = makeLabel(genreString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        genres.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel genrePanel = new JPanel(new BorderLayout());
        genrePanel.setBackground(new Color(247, 246, 242));
        genrePanel.add(genres);

        panel.add(genrePanel);

        JLabel rating = makeLabel("Average Rating: " + movie.getAverageRating().toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        rating.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ratingPanel = new JPanel(new BorderLayout());
        ratingPanel.setBackground(new Color(247, 246, 242));
        ratingPanel.add(rating);

        panel.add(ratingPanel);

        JLabel length = makeLabel("Length: " + movie.getMovieLength(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        length.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel lengthPanel = new JPanel(new BorderLayout());
        lengthPanel.setBackground(new Color(247, 246, 242));
        lengthPanel.add(length);

        panel.add(lengthPanel);

        JTextArea plot = new JTextArea("Plot: " + movie.getPlot());
        plot.setBackground(new Color(247, 246, 242));
        plot.setForeground(Color.BLACK);
        plot.setFont(new Font("Arial", Font.PLAIN, 15));
        plot.setEditable(false);
        plot.setWrapStyleWord(true);
        plot.setLineWrap(true);
        plot.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel plotPanel= new JPanel(new BorderLayout());
        plotPanel.setBackground(new Color(247, 246, 242));
        plotPanel.add(plot);

        panel.add(plotPanel);

        StringBuilder directorString = new StringBuilder("Directors: ");
        for (String director : movie.getDirectors()) {
            directorString.append(director);
            if (!director.equals(movie.getDirectors().getLast())) {
                directorString.append(", ");
            }
        }
        JLabel directors = makeLabel(directorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        directors.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel directorsPanel = new JPanel(new BorderLayout());
        directorsPanel.setBackground(new Color(247, 246, 242));
        directorsPanel.add(directors);

        panel.add(directorsPanel);

        StringBuilder actorString = new StringBuilder("Actors: ");
        for (String actor : movie.getActors()) {
            actorString.append(actor);
            if (!actor.equals(movie.getActors().getLast())) {
                actorString.append(", ");
            }
        }
        JLabel actors = makeLabel(actorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        actors.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel actorsPanel = new JPanel(new BorderLayout());
        actorsPanel.setBackground(new Color(247, 246, 242));
        actorsPanel.add(actors);
        panel.add(actorsPanel);

        JToggleButton favoritesButton = new JToggleButton();
        if (currentUser.getFavoriteProductions().contains(movie)){
            favoritesButton.setSelected(true);
            favoritesButton.setText("  Remove from Favorites  ");
            favoritesButton.setBackground(new Color(247, 246, 242));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
            favoritesButton.setForeground(new Color(81, 137, 124));
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        else {
            favoritesButton.setSelected(false);
            favoritesButton.setText("        Add to Favorites        ");
            favoritesButton.setBackground(new Color(81, 191, 164));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            favoritesButton.setForeground(Color.WHITE);
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        favoritesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                boolean selected = abstractButton.getModel().isSelected();

                if (selected) {
                    abstractButton.setText("  Remove from Favorites  ");
                    abstractButton.setBackground(new Color(247, 246, 242));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                    abstractButton.setForeground(new Color(81, 137, 124));
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.addFavoriteProduction(movie);
                } else {
                    abstractButton.setText("        Add to Favorites        ");
                    abstractButton.setBackground(new Color(81, 191, 164));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                    abstractButton.setForeground(Color.WHITE);
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.removeFavoriteProduction(movie);
                }
            }
        });
        JPanel toggleButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toggleButtonPanel.setBackground(new Color(247, 246, 242));
        toggleButtonPanel.add(favoritesButton);

        panel.add(toggleButtonPanel);

        JLabel ratingTitle = makeLabel("Ratings: ", Font.TRUETYPE_FONT, 15, 81, 191, 164);
        ratingTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ratingTitlePanel = new JPanel(new BorderLayout());
        ratingTitlePanel.setBackground(new Color(247, 246, 242));
        ratingTitlePanel.add(ratingTitle);
        panel.add(ratingTitlePanel);

        panel.add(new Label());

        for (Rating rating1 : movie.getRatings()){
            JLabel ratingGrade = makeLabel("    " + rating1.getUser() + ": " + rating1.getRating(), Font.BOLD, 15, 81, 191, 164);
            ratingGrade.setAlignmentX(Component.LEFT_ALIGNMENT);
            ratingGrade.setForeground(new Color(81, 191, 164));

            JPanel ratingGradePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ratingGradePanel.setBackground(new Color(247, 246, 242));
            ratingGradePanel.add(ratingGrade);

            if (rating1.getUser().equals(currentUser.getUsername())){
                JButton deleteRating = new JButton("Delete Rating");
                deleteRating.setPreferredSize(new Dimension(80, 30));
                deleteRating.setBackground(new Color(247, 246, 242));
                deleteRating.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                deleteRating.setForeground(new Color(81, 137, 124));
                deleteRating.addActionListener(e -> {
                    movie.removeRating(rating1);
                    JOptionPane.showMessageDialog(null, "Rating deleted successfully!");
                    movieFound(upperBorder, jPanel, frame, scrollPane, currentUser, movie);
                });
                ratingGradePanel.add(Box.createHorizontalStrut(50));
                ratingGradePanel.add(deleteRating);
            }

            panel.add(ratingGradePanel);

            JTextArea comment = new JTextArea("    " + rating1.getComment());
            comment.setBackground(new Color(247, 246, 242));
            comment.setForeground(Color.BLACK);
            comment.setFont(new Font("Arial", Font.PLAIN, 15));
            comment.setEditable(false);
            comment.setWrapStyleWord(true);
            comment.setLineWrap(true);
            comment.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel commentPanel = new JPanel(new BorderLayout());
            commentPanel.setBackground(new Color(247, 246, 242));
            commentPanel.add(comment);

            panel.add(commentPanel);

            panel.add(new Label());
        }

        if (currentUser instanceof Regular){
            panel.add(new Label());
            panel.add(new Label());

            JLabel addARating = makeLabel("Add a rating: ",Font.BOLD, 15, 81, 191, 164);
            JComboBox<Integer> grade = new JComboBox<>();
            grade.addItem(null);
            for (int i = 1; i < 11; i++){
                grade.addItem(i);
            }
            grade.setSelectedItem(null);
            JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            comboBoxPanel.setBackground(new Color(247, 246, 242));
            comboBoxPanel.add(addARating);
            comboBoxPanel.add(grade);

            panel.add(comboBoxPanel);

            JTextArea enterComment = new JTextArea("Enter comment", 5, 50);
            enterComment.setLineWrap(true);
            enterComment.setWrapStyleWord(true);
            enterComment.setBackground(new Color(235, 230, 226));
            enterComment.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
            enterComment.setForeground(Color.GRAY);
            enterComment.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (enterComment.getText().equals("Enter comment")) {
                        enterComment.setText("");
                        enterComment.setForeground(Color.BLACK);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (enterComment.getText().isEmpty()) {
                        enterComment.setForeground(Color.GRAY);
                        enterComment.setText("Enter comment");
                    }
                }
            });

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setBackground(new Color(247, 246, 242));

            searchPanel.add(Box.createHorizontalStrut(97));
            searchPanel.add(enterComment);

            panel.add(searchPanel);

            JButton finishButton = new JButton("Add Rating");
            finishButton.setPreferredSize(new Dimension(80, 30));
            finishButton.setBackground(new Color(81, 191, 164));
            finishButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            finishButton.setForeground(Color.WHITE);
            finishButton.addActionListener(e -> {
                if (movie.getRatings().stream().anyMatch(rat -> rat.getUser().equals(currentUser.getUsername()))){
                    JOptionPane.showMessageDialog(null, "Already rated this!");
                }
                else {
                    if (grade.getSelectedItem() == null) JOptionPane.showMessageDialog(null, "Invalid Grade!");
                    else {
                        ((Regular) currentUser).addRating(enterComment.getText(), (Integer) grade.getSelectedItem(), movie);
                        JOptionPane.showMessageDialog(null, "Rating added successfully!");
                        updateUpperBorder(upperBorder, currentUser);
                        movieFound(upperBorder, jPanel, frame, scrollPane, currentUser, movie);
                    }
                }
            });

            JPanel finishButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            finishButtonPanel.setBackground(new Color(247, 246, 242));

            finishButtonPanel.add(Box.createHorizontalStrut(97));
            finishButtonPanel.add(finishButton);

            panel.add(finishButtonPanel);
        }

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    private static void seriesFound(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser, Series currentSeries){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(247, 246, 242));

        buttonPanel.add(Box.createHorizontalStrut(900));
        buttonPanel.add(goBack);

        panel.add(buttonPanel);
        panel.add(new Label());

        JLabel title = makeLabel(currentSeries.getTitle() + " (" + currentSeries.getReleaseYear() + ")", Font.BOLD, 15, 81, 191, 164);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(new Color(247, 246, 242));
        titlePanel.add(title);

        if (currentUser instanceof Staff && ((Staff) currentUser).getProductionsAdded().contains(currentSeries)){
            JButton editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension( 80, 25));
            editButton.setBackground(new Color(81, 191, 164));
            editButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            editButton.setForeground(Color.WHITE);
            auxiliarySeries = currentSeries;
            editButton.addActionListener(e -> updateSeriesGUI(jPanel, frame, scrollPane, (Staff) currentUser, currentSeries));

            titlePanel.add(editButton);
        }

        panel.add(titlePanel);

        StringBuilder genreString = new StringBuilder("Genres: ");
        for (Genre genre : currentSeries.getGenres()) {
            genreString.append(genre.toString());
            if (!genre.equals(currentSeries.getGenres().getLast())) {
                genreString.append(", ");
            }
        }
        JLabel genres = makeLabel(genreString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        genres.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel genrePanel = new JPanel(new BorderLayout());
        genrePanel.setBackground(new Color(247, 246, 242));
        genrePanel.add(genres);

        panel.add(genrePanel);

        JLabel rating = makeLabel("Average Rating: " + currentSeries.getAverageRating().toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        rating.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ratingPanel = new JPanel(new BorderLayout());
        ratingPanel.setBackground(new Color(247, 246, 242));
        ratingPanel.add(rating, BorderLayout.CENTER);

        panel.add(ratingPanel);

        JLabel length = makeLabel("Number of seasons: " + currentSeries.getSeasonsNumber(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        length.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel lengthLabel = new JPanel(new BorderLayout());
        lengthLabel.setBackground(new Color(247, 246, 242));
        lengthLabel.add(length, BorderLayout.CENTER);

        panel.add(lengthLabel);

        JTextArea plot = new JTextArea("Plot: " + currentSeries.getPlot());
        plot.setBackground(new Color(247, 246, 242));
        plot.setForeground(Color.BLACK);
        plot.setFont(new Font("Arial", Font.PLAIN, 15));
        plot.setEditable(false);
        plot.setWrapStyleWord(true);
        plot.setLineWrap(true);
        plot.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel plotPanel = new JPanel(new BorderLayout());
        plotPanel.setBackground(new Color(247, 246, 242));
        plotPanel.add(plot, BorderLayout.CENTER);

        panel.add(plotPanel);

        StringBuilder directorString = new StringBuilder("Directors: ");
        for (String director : currentSeries.getDirectors()) {
            directorString.append(director);
            if (!director.equals(currentSeries.getDirectors().getLast())) {
                directorString.append(", ");
            }
        }
        JLabel directors = makeLabel(directorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        directors.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel directorsPanel = new JPanel(new BorderLayout());
        directorsPanel.setBackground(new Color(247, 246, 242));
        directorsPanel.add(directors, BorderLayout.CENTER);

        panel.add(directorsPanel);

        StringBuilder actorString = new StringBuilder("Actors: ");
        for (String actor : currentSeries.getActors()) {
            actorString.append(actor);
            if (!actor.equals(currentSeries.getActors().getLast())) {
                actorString.append(", ");
            }
        }
        JLabel actors = makeLabel(actorString.toString(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
        actors.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel actorsPanel = new JPanel(new BorderLayout());
        actorsPanel.setBackground(new Color(247, 246, 242));
        actorsPanel.add(actors, BorderLayout.CENTER);

        panel.add(actorsPanel);

        JToggleButton favoritesButton = new JToggleButton();
        if (currentUser.getFavoriteProductions().contains(currentSeries)){
            favoritesButton.setSelected(true);
            favoritesButton.setText("  Remove from Favorites  ");
            favoritesButton.setBackground(new Color(247, 246, 242));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
            favoritesButton.setForeground(new Color(81, 137, 124));
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        else {
            favoritesButton.setSelected(false);
            favoritesButton.setText("        Add to Favorites        ");
            favoritesButton.setBackground(new Color(81, 191, 164));
            favoritesButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            favoritesButton.setForeground(Color.WHITE);
            favoritesButton.setFont(new Font("Arial", Font.BOLD, 15));
        }
        favoritesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                boolean selected = abstractButton.getModel().isSelected();

                if (selected) {
                    abstractButton.setText("  Remove from Favorites  ");
                    abstractButton.setBackground(new Color(247, 246, 242));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                    abstractButton.setForeground(new Color(81, 137, 124));
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.addFavoriteProduction(currentSeries);
                } else {
                    abstractButton.setText("        Add to Favorites        ");
                    abstractButton.setBackground(new Color(81, 191, 164));
                    abstractButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                    abstractButton.setForeground(Color.WHITE);
                    abstractButton.setFont(new Font("Arial", Font.BOLD, 15));
                    currentUser.removeFavoriteProduction(currentSeries);
                }
            }
        });

        JPanel toggleButtonLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toggleButtonLabel.setBackground(new Color(247, 246, 242));
        toggleButtonLabel.add(favoritesButton);

        panel.add(toggleButtonLabel);

        panel.add(new Label());

        for (Map.Entry<String, List<Episode>> entry : currentSeries.getSeasons().entrySet()) {
            String seasonNumber = entry.getKey();
            List<Episode> episodes = entry.getValue();

            JLabel season = makeLabel(seasonNumber + ":", Font.TRUETYPE_FONT, 15, 81, 191, 164);
            season.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            seasonsPanel.setBackground(new Color(247, 246, 242));
            seasonsPanel.add(season);

            panel.add(seasonsPanel);
            int i = 0;
            for (Episode episode : episodes) {
                i++;
                JLabel ep = makeLabel("    Episode " + i + " - " + episode.getEpisodeName(), Font.TRUETYPE_FONT, 15, 81, 191, 164);
                ep.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel episodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                episodePanel.setBackground(new Color(247, 246, 242));
                episodePanel.add(ep);

                panel.add(episodePanel);
            }
        }

        panel.add(new Label());

        JLabel ratingTitle = makeLabel("Ratings: ", Font.TRUETYPE_FONT, 15, 81, 191, 164);
        ratingTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ratingTitlePanel = new JPanel(new BorderLayout());
        ratingTitlePanel.setBackground(new Color(247, 246, 242));
        ratingTitlePanel.add(ratingTitle);
        panel.add(ratingTitlePanel);

        panel.add(new Label());

        for (Rating rating1 : currentSeries.getRatings()){
            JLabel ratingGrade = makeLabel("    " + rating1.getUser() + ": " + rating1.getRating(), Font.BOLD, 15, 81, 191, 164);
            ratingGrade.setAlignmentX(Component.LEFT_ALIGNMENT);
            ratingGrade.setForeground(new Color(81, 191, 164));

            JPanel ratingGradePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ratingGradePanel.setBackground(new Color(247, 246, 242));
            ratingGradePanel.add(ratingGrade);

            if (rating1.getUser().equals(currentUser.getUsername())){
                JButton deleteRating = new JButton("Delete Rating");
                deleteRating.setPreferredSize(new Dimension(80, 30));
                deleteRating.setBackground(new Color(247, 246, 242));
                deleteRating.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                deleteRating.setForeground(new Color(81, 137, 124));
                deleteRating.addActionListener(e -> {
                    currentSeries.removeRating(rating1);
                    JOptionPane.showMessageDialog(null, "Rating deleted successfully!");
                    seriesFound(upperBorder, jPanel, frame, scrollPane, currentUser, currentSeries);
                });
                ratingGradePanel.add(Box.createHorizontalStrut(50));
                ratingGradePanel.add(deleteRating);
            }

            panel.add(ratingGradePanel);

            JTextArea comment = new JTextArea("    " + rating1.getComment());
            comment.setBackground(new Color(247, 246, 242));
            comment.setForeground(Color.BLACK);
            comment.setFont(new Font("Arial", Font.PLAIN, 15));
            comment.setEditable(false);
            comment.setWrapStyleWord(true);
            comment.setLineWrap(true);
            comment.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel commentPanel = new JPanel(new BorderLayout());
            commentPanel.setBackground(new Color(247, 246, 242));
            commentPanel.add(comment);

            panel.add(commentPanel);

            panel.add(new Label());
        }

        if (currentUser instanceof Regular){
            panel.add(new Label());
            panel.add(new Label());

            JLabel addARating = makeLabel("Add a rating: ",Font.BOLD, 15, 81, 191, 164);
            JComboBox<Integer> grade = new JComboBox<>();
            grade.addItem(null);
            for (int i = 1; i < 11; i++){
                grade.addItem(i);
            }
            grade.setSelectedItem(null);
            JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            comboBoxPanel.setBackground(new Color(247, 246, 242));
            comboBoxPanel.add(addARating);
            comboBoxPanel.add(grade);

            panel.add(comboBoxPanel);

            JTextArea enterComment = new JTextArea("Enter comment", 5, 50);
            enterComment.setLineWrap(true);
            enterComment.setWrapStyleWord(true);
            enterComment.setBackground(new Color(235, 230, 226));
            enterComment.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
            enterComment.setForeground(Color.GRAY);
            enterComment.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (enterComment.getText().equals("Enter comment")) {
                        enterComment.setText("");
                        enterComment.setForeground(Color.BLACK);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (enterComment.getText().isEmpty()) {
                        enterComment.setForeground(Color.GRAY);
                        enterComment.setText("Enter comment");
                    }
                }
            });

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setBackground(new Color(247, 246, 242));

            searchPanel.add(Box.createHorizontalStrut(97));
            searchPanel.add(enterComment);

            panel.add(searchPanel);

            JButton finishButton = new JButton("Add Rating");
            finishButton.setPreferredSize(new Dimension(80, 30));
            finishButton.setBackground(new Color(81, 191, 164));
            finishButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
            finishButton.setForeground(Color.WHITE);
            finishButton.addActionListener(e -> {
                if (currentSeries.getRatings().stream().anyMatch(rat -> rat.getUser().equals(currentUser.getUsername()))){
                    JOptionPane.showMessageDialog(null, "Already rated this!");
                }
                else {
                    if (grade.getSelectedItem() == null) JOptionPane.showMessageDialog(null, "Invalid Grade!");
                    else {
                        ((Regular) currentUser).addRating(enterComment.getText(), (Integer) grade.getSelectedItem(), currentSeries);
                        JOptionPane.showMessageDialog(null, "Rating created successfully!");
                        updateUpperBorder(upperBorder, currentUser);
                        seriesFound(upperBorder, jPanel, frame, scrollPane, currentUser, currentSeries);
                    }
                }
            });

            JPanel finishButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            finishButtonPanel.setBackground(new Color(247, 246, 242));

            finishButtonPanel.add(Box.createHorizontalStrut(97));
            finishButtonPanel.add(finishButton);

            panel.add(finishButtonPanel);
        }


        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void makeARequest(JPanel jPanel, JFrame frame, JPanel scrollPane, User currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(900));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        panel.add(upperButtons);
        panel.add(new Label());

        int requestCounter = 0;

        for (Request request : Admin.RequestsHolder.getTeamRequests()){
            if (request.getCreatorUsername().equals(currentUser.getUsername())){
                requestCounter++;
                String title = "Request addressed to the administration team regarding ";
                if (request.getRequestType().equals(RequestTypes.DELETE_ACCOUNT)){
                    title += "ACCOUNT DELETION";
                }
                else if (request.getRequestType().equals(RequestTypes.OTHERS)){
                    title += "OTHER REASONS";
                }
                JPanel requestTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
                requestTitle.setBackground(new Color(247, 246, 242));
                requestTitle.add(makeLabel(title, Font.BOLD, 15, 81, 191, 164));
                panel.add(requestTitle);

                JTextArea requestDescription = new JTextArea(request.getDescription());
                requestDescription.setBackground(new Color(247, 246, 242));
                requestDescription.setForeground(Color.BLACK);
                requestDescription.setFont(new Font("Arial", Font.PLAIN, 15));
                requestDescription.setEditable(false);
                requestDescription.setWrapStyleWord(true);
                requestDescription.setLineWrap(true);
                requestDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel descriptionPanel = new JPanel(new BorderLayout());
                descriptionPanel.setBackground(new Color(247, 246, 242));
                descriptionPanel.add(requestDescription, BorderLayout.CENTER);

                panel.add(descriptionPanel);
                panel.add(new Label());
            }
        }
        for (Admin admin : adminList){
            for (Request request : admin.getOwnList()){
                if (request.getCreatorUsername().equals(currentUser.getUsername())){
                    requestCounter++;
                    String title = "Request addressed to " + request.getSolverUsername() + " regarding ";
                    if (request.getRequestType().equals(RequestTypes.MOVIE_ISSUE)){
                        title += "A MOVIE ISSUE(" + request.getTitleOrMovie() + ")";
                    }
                    else if (request.getRequestType().equals(RequestTypes.ACTOR_ISSUE)){
                        title += "AN ACTOR ISSUE (" + request.getTitleOrMovie() + ")";
                    }
                    JPanel requestTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    requestTitle.setBackground(new Color(247, 246, 242));
                    requestTitle.add(makeLabel(title, Font.BOLD, 15, 81, 191, 164));
                    panel.add(requestTitle);

                    JTextArea requestDescription = new JTextArea(request.getDescription());
                    requestDescription.setBackground(new Color(247, 246, 242));
                    requestDescription.setForeground(Color.BLACK);
                    requestDescription.setFont(new Font("Arial", Font.PLAIN, 15));
                    requestDescription.setEditable(false);
                    requestDescription.setWrapStyleWord(true);
                    requestDescription.setLineWrap(true);
                    requestDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel descriptionPanel = new JPanel(new BorderLayout());
                    descriptionPanel.setBackground(new Color(247, 246, 242));
                    descriptionPanel.add(requestDescription, BorderLayout.CENTER);

                    panel.add(descriptionPanel);
                    panel.add(new Label());
                }
            }
        }

        for (Contributor contributor : contributorList){
            for (Request request : contributor.getOwnList()){
                if (request.getCreatorUsername().equals(currentUser.getUsername())){
                    requestCounter++;
                    String title = "Request addressed to " + request.getSolverUsername() + " regarding ";
                    if (request.getRequestType().equals(RequestTypes.MOVIE_ISSUE)){
                        title += "A MOVIE ISSUE(" + request.getTitleOrMovie() + ")";
                    }
                    else if (request.getRequestType().equals(RequestTypes.ACTOR_ISSUE)){
                        title += "AN ACTOR ISSUE (" + request.getTitleOrMovie() + ")";
                    }
                    JPanel requestTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    requestTitle.setBackground(new Color(247, 246, 242));
                    requestTitle.add(makeLabel(title, Font.BOLD, 15, 81, 191, 164));
                    panel.add(requestTitle);

                    JTextArea requestDescription = new JTextArea(request.getDescription());
                    requestDescription.setBackground(new Color(247, 246, 242));
                    requestDescription.setForeground(Color.BLACK);
                    requestDescription.setFont(new Font("Arial", Font.PLAIN, 15));
                    requestDescription.setEditable(false);
                    requestDescription.setWrapStyleWord(true);
                    requestDescription.setLineWrap(true);
                    requestDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel descriptionPanel = new JPanel(new BorderLayout());
                    descriptionPanel.setBackground(new Color(247, 246, 242));
                    descriptionPanel.add(requestDescription, BorderLayout.CENTER);

                    panel.add(descriptionPanel);
                    panel.add(new Label());
                }
            }
        }

        if(requestCounter <= 0){
            JPanel requestTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
            requestTitle.setBackground(new Color(247, 246, 242));
            requestTitle.add(makeLabel("No active requests.", Font.BOLD, 15, 81, 191, 164));
            panel.add(requestTitle);
        }
        panel.add(new Label());
        panel.add(new Label());

        JLabel makeARequest = makeLabel("Make a request: ",Font.BOLD, 15, 81, 191, 164);

        JComboBox<RequestTypes> requestTypesJComboBox = new JComboBox<>(RequestTypes.values());
        requestTypesJComboBox.addItem(null);
        requestTypesJComboBox.setSelectedItem(null);


        JComboBox<Actor> actorJComboBox = new JComboBox<>(actors.toArray(new Actor[0]));
        actorJComboBox.addItem(null);
        actorJComboBox.setSelectedItem(null);
        actorJComboBox.setVisible(false);

        List<Production> allProductions = Stream.concat(movies.stream(), series.stream())
                .toList();
        JComboBox<Production> productionJComboBox = new JComboBox<>(allProductions.toArray(new Production[0]));
        productionJComboBox.addItem(null);
        productionJComboBox.setSelectedItem(null);
        productionJComboBox.setVisible(false);

        requestTypesJComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    RequestTypes selectedOption = (RequestTypes) e.getItem();
                    actorJComboBox.setVisible(RequestTypes.ACTOR_ISSUE.equals(selectedOption));
                    productionJComboBox.setVisible(RequestTypes.MOVIE_ISSUE.equals(selectedOption));
                    if (!RequestTypes.ACTOR_ISSUE.equals(selectedOption)) {
                        actorJComboBox.setSelectedItem(null);
                    }
                    if (!RequestTypes.MOVIE_ISSUE.equals(selectedOption)) {
                        productionJComboBox.setSelectedItem(null);
                    }
                }
                else {
                    productionJComboBox.setSelectedItem(null);
                    productionJComboBox.setVisible(false);
                    actorJComboBox.setSelectedItem(null);
                    actorJComboBox.setVisible(false);
                }
            }
        });

        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBoxPanel.setBackground(new Color(247, 246, 242));
        comboBoxPanel.add(makeARequest);
        comboBoxPanel.add(requestTypesJComboBox);
        comboBoxPanel.add(actorJComboBox);
        comboBoxPanel.add(productionJComboBox);



        panel.add(comboBoxPanel);

        JTextArea enterComment = new JTextArea("Write a description", 5, 50);
        enterComment.setLineWrap(true);
        enterComment.setWrapStyleWord(true);
        enterComment.setBackground(new Color(235, 230, 226));
        enterComment.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        enterComment.setForeground(Color.GRAY);
        enterComment.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (enterComment.getText().equals("Write a description")) {
                    enterComment.setText("");
                    enterComment.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (enterComment.getText().isEmpty()) {
                    enterComment.setForeground(Color.GRAY);
                    enterComment.setText("Write a description");
                }
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(new Color(247, 246, 242));

        searchPanel.add(Box.createHorizontalStrut(97));
        searchPanel.add(enterComment);

        panel.add(searchPanel);

        JButton finishButton = new JButton("Create Request");
        finishButton.setPreferredSize(new Dimension(120, 30));
        finishButton.setBackground(new Color(81, 191, 164));
        finishButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finishButton.setForeground(Color.WHITE);
        finishButton.addActionListener(e -> {
            if (requestTypesJComboBox.getSelectedItem() == null) JOptionPane.showMessageDialog(null, "Please select a valid option");
            else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                String solverName = "";
                if (requestTypesJComboBox.getSelectedItem().equals(RequestTypes.OTHERS) || requestTypesJComboBox.getSelectedItem().equals(RequestTypes.DELETE_ACCOUNT)) {
                    solverName = "ADMIN";
                } else {
                    if (actorJComboBox.getSelectedItem() == null && productionJComboBox.getSelectedItem() == null)
                        JOptionPane.showMessageDialog(null, "Please select a valid option");
                    else {
                        if (requestTypesJComboBox.getSelectedItem().equals(RequestTypes.ACTOR_ISSUE)) {
                            for (Admin admin : adminList) {
                                if (admin.getActorsAdded().contains(actorJComboBox.getSelectedItem())){
                                    solverName = admin.getUsername();
                                    break;
                                }
                            }
                            if (solverName.isEmpty()){
                                for (Contributor contributor : contributorList) {
                                    if (contributor.getActorsAdded().contains(actorJComboBox.getSelectedItem())){
                                        solverName = contributor.getUsername();
                                        break;
                                    }
                                }
                            }
                        }
                        else if(requestTypesJComboBox.getSelectedItem().equals(RequestTypes.MOVIE_ISSUE)){
                            for (Admin admin : adminList) {
                                if (admin.getProductionsAdded().contains(productionJComboBox.getSelectedItem())){
                                    solverName = admin.getUsername();
                                    break;
                                }
                            }
                            if (solverName.isEmpty()){
                                for (Contributor contributor : contributorList) {
                                    if (contributor.getProductionsAdded().contains(productionJComboBox.getSelectedItem())){
                                        solverName = contributor.getUsername();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (solverName.equals(currentUser.getUsername())){
                    JOptionPane.showMessageDialog(null, "You can't create a request addressed to yourself!");
                }
                else {
                    if (currentUser instanceof Regular) {
                        if(solverName.equals("ADMIN")) {
                            ((Regular) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName));
                            JOptionPane.showMessageDialog(null, "Request created successfully!");
                            makeARequest(jPanel, frame, scrollPane, currentUser);
                        }
                            else {
                            String titleName = "";
                            if (productionJComboBox.getSelectedItem() == null && actorJComboBox.getSelectedItem() != null) {
                                titleName = actorJComboBox.getSelectedItem().toString();
                                ((Regular) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName, titleName));
                                JOptionPane.showMessageDialog(null, "Request created successfully!");
                                makeARequest(jPanel, frame, scrollPane, currentUser);
                            }
                            else if (actorJComboBox.getSelectedItem() == null && productionJComboBox.getSelectedItem() != null){
                                titleName = productionJComboBox.getSelectedItem().toString();
                                ((Regular) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName, titleName));
                                JOptionPane.showMessageDialog(null, "Request created successfully!");
                                makeARequest(jPanel, frame, scrollPane, currentUser);
                            }
                        }
                    }
                    else {
                        if(solverName.equals("ADMIN")) {
                            ((Contributor) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName));
                            JOptionPane.showMessageDialog(null, "Request created successfully!");
                            makeARequest(jPanel, frame, scrollPane, currentUser);
                        }
                        else {
                            String titleName = "";
                            if (productionJComboBox.getSelectedItem() == null && actorJComboBox.getSelectedItem() != null) {
                                titleName = actorJComboBox.getSelectedItem().toString();
                                ((Contributor) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName, titleName));
                                JOptionPane.showMessageDialog(null, "Request created successfully!");
                                makeARequest(jPanel, frame, scrollPane, currentUser);
                            }
                            else if (actorJComboBox.getSelectedItem() == null && productionJComboBox.getSelectedItem() != null){
                                titleName = actorJComboBox.getSelectedItem().toString();
                                ((Contributor) currentUser).createRequest(new Request(requestTypesJComboBox.getSelectedItem().toString(), LocalDateTime.now().format(formatter), enterComment.getText(), currentUser.getUsername(), solverName, titleName));
                                JOptionPane.showMessageDialog(null, "Request created successfully!");
                                makeARequest(jPanel, frame, scrollPane, currentUser);
                            }
                        }
                    }
                }
            }
        });

        JPanel finishButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        finishButtonPanel.setBackground(new Color(247, 246, 242));

        finishButtonPanel.add(Box.createHorizontalStrut(97));
        finishButtonPanel.add(finishButton);

        panel.add(finishButtonPanel);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void solveARequestGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(900));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        panel.add(upperButtons);


        if ((currentUser instanceof Contributor && currentUser.getOwnList().isEmpty()) || (currentUser instanceof Admin && Admin.RequestsHolder.getTeamRequests().isEmpty() && currentUser.getOwnList().isEmpty())) {
            JPanel noReq = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            noReq.setBackground(new Color(247, 246, 242));
            noReq.add(makeLabel("No new requests", Font.BOLD, 15, 81, 191, 164));
            panel.add(noReq);
        }
        else {
            if (currentUser instanceof Admin){
                for (Request r : Admin.RequestsHolder.getTeamRequests()){
                    JPanel currentRequest = new JPanel(new FlowLayout((FlowLayout.LEFT)));
                    currentRequest.setBackground(new Color(247, 246, 242));
                    String request = "Request made by " + r.getCreatorUsername() + " at " + r.getTime().toString() + " regarding ";
                    switch (r.getRequestType()) {
                        case ACTOR_ISSUE:
                            request += "an issue with an ACTOR (" + r.getTitleOrMovie() + ")\n";
                            break;
                        case MOVIE_ISSUE:
                            request += "an issue with a PRODUCTION (" + r.getTitleOrMovie() + ")\n";
                            break;
                        case DELETE_ACCOUNT:
                            request += "the DELETION OF AN ACCOUNT\n";
                            break;
                        case OTHERS:
                            request += "OTHER REASONS\n";
                            break;
                    }
                    JLabel requestLabel = makeLabel(request, Font.BOLD, 15, 81, 191, 164);
                    currentRequest.add(requestLabel);

                    panel.add(currentRequest);

                    JTextArea requestDescription = new JTextArea(r.getDescription());
                    requestDescription.setBackground(new Color(247, 246, 242));
                    requestDescription.setForeground(Color.BLACK);
                    requestDescription.setFont(new Font("Arial", Font.PLAIN, 15));
                    requestDescription.setEditable(false);
                    requestDescription.setWrapStyleWord(true);
                    requestDescription.setLineWrap(true);
                    requestDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel descriptionPanel = new JPanel(new BorderLayout());
                    descriptionPanel.setBackground(new Color(247, 246, 242));
                    descriptionPanel.add(requestDescription, BorderLayout.CENTER);
                    panel.add(descriptionPanel);

                    JPanel solveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    solveButtons.setBackground(new Color(247, 246, 242));

                    JButton solvedButton = new JButton("SOLVE");
                    solvedButton.setBounds(50, 195, 80, 25);
                    solvedButton.setBackground(new Color(81, 191, 164));
                    solvedButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                    solvedButton.setForeground(Color.WHITE);
                    solvedButton.addActionListener(e -> {
                        currentUser.solveRequest(r, "SOLVED");
                        JOptionPane.showMessageDialog(null, "Request marked successfully!");
                        solveARequestGUI(jPanel, frame, scrollPane, currentUser);
                    });
                    solveButtons.add(solvedButton);

                    JButton rejectedButton = new JButton("REJECT");
                    rejectedButton.setBounds(170, 195, 80, 25);
                    rejectedButton.setBackground(new Color(247, 246, 242));
                    rejectedButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                    rejectedButton.setForeground(new Color(81, 137, 124));
                    rejectedButton.addActionListener(e -> {
                        currentUser.solveRequest(r, "REJECTED");
                        JOptionPane.showMessageDialog(null, "Request marked successfully!");
                        solveARequestGUI(jPanel, frame, scrollPane, currentUser);
                    });
                    solveButtons.add(rejectedButton);

                    panel.add(solveButtons);
                    panel.add(new Label());
                }
            }
            for (Request r : currentUser.getOwnList()){
                JPanel currentRequest = new JPanel(new FlowLayout((FlowLayout.LEFT)));
                currentRequest.setBackground(new Color(247, 246, 242));
                String request = "Request made by " + r.getCreatorUsername() + " at " + r.getTime().toString() + " regarding ";
                switch (r.getRequestType()) {
                    case ACTOR_ISSUE:
                        request += "an issue with an ACTOR (" + r.getTitleOrMovie() + ")\n";
                        break;
                    case MOVIE_ISSUE:
                        request += "an issue with a PRODUCTION (" + r.getTitleOrMovie() + ")\n";
                        break;
                    case DELETE_ACCOUNT:
                        request += "the DELETION OF AN ACCOUNT\n";
                        break;
                    case OTHERS:
                        request += "OTHER REASONS\n";
                        break;
                }
                JLabel requestLabel = makeLabel(request, Font.BOLD, 15, 81, 191, 164);
                currentRequest.add(requestLabel);

                panel.add(currentRequest);

                JTextArea requestDescription = new JTextArea(r.getDescription());
                requestDescription.setBackground(new Color(247, 246, 242));
                requestDescription.setForeground(Color.BLACK);
                requestDescription.setFont(new Font("Arial", Font.PLAIN, 15));
                requestDescription.setEditable(false);
                requestDescription.setWrapStyleWord(true);
                requestDescription.setLineWrap(true);
                requestDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel descriptionPanel = new JPanel(new BorderLayout());
                descriptionPanel.setBackground(new Color(247, 246, 242));
                descriptionPanel.add(requestDescription, BorderLayout.CENTER);

                panel.add(descriptionPanel);

                JPanel solveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
                solveButtons.setBackground(new Color(247, 246, 242));

                JButton solvedButton = new JButton("SOLVE");
                solvedButton.setBounds(50, 195, 80, 25);
                solvedButton.setBackground(new Color(81, 191, 164));
                solvedButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
                solvedButton.setForeground(Color.WHITE);
                solvedButton.addActionListener(e -> {
                    currentUser.solveRequest(r, "SOLVED");
                    JOptionPane.showMessageDialog(null, "Request marked successfully!");
                    solveARequestGUI(jPanel, frame, scrollPane, currentUser);
                });
                solveButtons.add(solvedButton);

                JButton rejectedButton = new JButton("REJECT");
                rejectedButton.setBounds(170, 195, 80, 25);
                rejectedButton.setBackground(new Color(247, 246, 242));
                rejectedButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
                rejectedButton.setForeground(new Color(81, 137, 124));
                rejectedButton.addActionListener(e -> {
                    currentUser.solveRequest(r, "REJECTED");
                    JOptionPane.showMessageDialog(null, "Request marked successfully!");
                    solveARequestGUI(jPanel, frame, scrollPane, currentUser);
                });
                solveButtons.add(rejectedButton);

                panel.add(solveButtons);
                panel.add(new Label());
            }
        }
        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void removeActorProduction(JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser){

        String[] options = {"Actor", "Production"};

        JComboBox<String> removeType = new JComboBox<>(options);
        removeType.addItem(null);
        removeType.setSelectedItem(null);


        JComboBox<Actor> actorJComboBox = new JComboBox<>(currentUser.getActorsAdded().toArray(new Actor[0]));
        actorJComboBox.addItem(null);
        actorJComboBox.setSelectedItem(null);
        actorJComboBox.setVisible(false);

        JComboBox<Production> productionJComboBox = new JComboBox<>(currentUser.getProductionsAdded().toArray(new Production[0]));
        productionJComboBox.addItem(null);
        productionJComboBox.setSelectedItem(null);
        productionJComboBox.setVisible(false);

        JLabel label1 = new JLabel("Select what to remove:");
        JLabel label2 = new JLabel("Select which actor to remove:");
        label2.setVisible(false);
        JLabel label3 = new JLabel("Select which production to remove:");
        label3.setVisible(false);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));

        panel.add(label1);
        panel.add(removeType);
        panel.add(label2);
        panel.add(actorJComboBox);
        panel.add(label3);
        panel.add(productionJComboBox);

        removeType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedOption = (String) removeType.getSelectedItem();

                actorJComboBox.setVisible("Actor".equals(selectedOption));
                label2.setVisible("Actor".equals(selectedOption));
                productionJComboBox.setVisible("Production".equals(selectedOption));
                label3.setVisible("Production".equals(selectedOption));

                if (selectedOption == null) {
                    actorJComboBox.setSelectedItem(null);
                    productionJComboBox.setSelectedItem(null);
                    actorJComboBox.setVisible(false);
                    label2.setVisible(false);
                    productionJComboBox.setVisible(false);
                    label3.setVisible(false);
                }
            }
        });

        actorJComboBox.setVisible(false);
        productionJComboBox.setVisible(false);

        int result = JOptionPane.showOptionDialog(null, panel, "Remove actor/production",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            if (removeType.getSelectedItem() == null || (removeType.getSelectedItem().equals("Actor") && actorJComboBox.getSelectedItem() == null) || (removeType.getSelectedItem().equals("Production") && productionJComboBox.getSelectedItem() == null)){
                JOptionPane.showMessageDialog(null, "Please select a valid option");
            }
            else if (removeType.getSelectedItem().equals("Actor")){
                currentUser.removeActorSystem(actorJComboBox.getSelectedItem().toString());
                JOptionPane.showMessageDialog(null, "Actor removed successfully!");
            }
            else {
                currentUser.removeProductionSystem(productionJComboBox.getSelectedItem().toString());
                JOptionPane.showMessageDialog(null, "Production removed successfully!");
            }
        }
    }
    public static void addActorProductionGUI(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser){
        String[] options = {"Actor", "Movie", "Series"};

        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.addItem(null);
        comboBox.setSelectedItem(null);

        // Show the pop-up
        int result = JOptionPane.showOptionDialog(
                null,
                comboBox,
                "Select what to add in the system",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        );

        // Handle the OK button click
        if (result == JOptionPane.OK_OPTION) {
            String selection = (String) comboBox.getSelectedItem();

            if (selection == null) JOptionPane.showMessageDialog(null, "Please select a valid option");
            if (selection.equals("Actor")) addActorGUI(upperBorder, jPanel, frame, scrollPane, currentUser, new ArrayList<>(), new ArrayList<>(),"", "Enter biography");
            if (selection.equals("Movie")) addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),"", "Enter synopsis", 2000, "");
            if (selection.equals("Series")) addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),"", "Enter synopsis", 2000, 0, new TreeMap<>());
        }
    }
    public static void addActorGUI(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, List<String> performanceNames, List<String> performanceTypes, String nameText, String bioText){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JTextField nameFieldText = new JTextField(nameText);
        nameFieldText.setPreferredSize(new Dimension(100, 30));
        nameFieldText.setBackground(new Color(235, 230, 226));
        nameFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        nameFieldText.setForeground(Color.BLACK);

        JTextArea bioField = new JTextArea(bioText);
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (bioText.equals("Enter biography")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter biography")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter biography");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nameFieldText.getText().isEmpty() || bioField.getText().equals("Enter biography")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    boolean found = false;
                    for (Actor actor : actors){
                        if (actor.getActorName().equals(nameFieldText.getText())){
                            found = true;
                            break;
                        }
                    }
                    if (found) JOptionPane.showMessageDialog(null, "Actor already exists!");
                    else {
                        Actor actor = new Actor(nameFieldText.getText(), bioField.getText());
                        for (int i = 0; i < performanceNames.size(); i++) {
                            actor.addPerformance(performanceNames.get(i), performanceTypes.get(i));
                        }
                        currentUser.addActorSystem(actor);
                        JOptionPane.showMessageDialog(null, "Actor added successfully!");
                        jPanel.removeAll();
                        mainMenuComponents(jPanel, frame, currentUser);
                    }
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: ", Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        namePanel.add(nameFieldText);

        panel.add(namePanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel performancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        performancePanel.setBackground(new Color(247, 246, 242));

        JButton addPerformanceButton = new JButton("Add Performance");
        addPerformanceButton.setPreferredSize(new Dimension(200, 40));
        addPerformanceButton.setBackground(new Color(81, 191, 164));
        addPerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addPerformanceButton.setForeground(Color.WHITE);
        addPerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        addPerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();
                JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Movie", "Series"});

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Performance Name:"));
                popupPanel.add(nameField);
                popupPanel.add(new JLabel("Performance Type:"));
                popupPanel.add(typeComboBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a performance",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String performanceName = nameField.getText();
                    String performanceType = (String) typeComboBox.getSelectedItem();

                    performanceNames.add(performanceName);
                    performanceTypes.add(performanceType);
                    JOptionPane.showMessageDialog(null, "Performance added successfully!");
                    addActorGUI(upperBorder, jPanel, frame, scrollPane, currentUser, performanceNames, performanceTypes, nameFieldText.getText(), bioField.getText());
                }
            }
        });
        performancePanel.add(addPerformanceButton);

        panel.add(performancePanel);

        JPanel removePerformancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removePerformancePanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Performance");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (performanceNames.size() <= 0){
                    JOptionPane.showMessageDialog(null, "No performances added yet!.");
                }
                else {
                    JComboBox<String> nameComboBox = new JComboBox<>(performanceNames.toArray(new String[0]));

                    JPanel popupPanel = new JPanel();
                    popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                    popupPanel.add(new JLabel("Performance Name:"));
                    popupPanel.add(nameComboBox);

                    int result = JOptionPane.showOptionDialog(
                            null,
                            popupPanel,
                            "Remove a performance",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            null
                    );
                    if (result == JOptionPane.OK_OPTION) {
                        String performanceName = (String) nameComboBox.getSelectedItem();
                        int i = performanceNames.indexOf(performanceName);
                        performanceNames.remove(i);
                        performanceTypes.remove(i);
                        JOptionPane.showMessageDialog(null, "Performance removed successfully!");
                        addActorGUI(upperBorder, jPanel, frame, scrollPane, currentUser, performanceNames, performanceTypes, nameFieldText.getText(), bioField.getText());
                    }
                }
            }
        });
        removePerformancePanel.add(removePerformanceButton);

        panel.add(removePerformancePanel);

        if (performanceNames.isEmpty()) {
            JPanel performancesPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            performancesPanel.setBackground(new Color(247, 246, 242));
            performancesPanel.add(makeLabel("Performances: None", Font.BOLD, 15, 81, 191, 164));
            panel.add(performancesPanel);
        }
        else {
            for (int i = 0; i < performanceNames.size(); i++){
                JPanel performancesPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
                performancesPanel.setBackground(new Color(247, 246, 242));
                if (i == 0){
                    performancesPanel.add(makeLabel("Performances: ", Font.BOLD, 15, 81, 191, 164));
                }
                else {
                    performancesPanel.add(Box.createHorizontalStrut(110));
                }
                performancesPanel.add(makeLabel(performanceNames.get(i) + " - " + performanceTypes.get(i), Font.BOLD, 15, 81, 191, 164));
                panel.add(performancesPanel);
            }
        }


        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void addMovieGUI(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, List<String> directorNames, List<String> actorNames, List<Genre> genreList, String nameText, String bioText, int year, String length){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JTextField nameFieldText = new JTextField(nameText);
        nameFieldText.setPreferredSize(new Dimension(100, 30));
        nameFieldText.setBackground(new Color(235, 230, 226));
        nameFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        nameFieldText.setForeground(Color.BLACK);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, currentYear - 100, currentYear + 100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setValue(year);

        JTextField lengthText = new JTextField(length);
        lengthText.setPreferredSize(new Dimension(100, 30));
        lengthText.setBackground(new Color(235, 230, 226));
        lengthText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        lengthText.setForeground(Color.BLACK);

        JTextArea bioField = new JTextArea(bioText);
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (bioText.equals("Enter synopsis")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter synopsis")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter synopsis");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lengthText.getText().isEmpty() || nameFieldText.getText().isEmpty() || bioField.getText().equals("Enter synopsis")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    boolean found = false;
                    for (Movie movie : movies){
                        if (movie.getTitle().equals(nameFieldText.getText())){
                            found = true;
                            break;
                        }
                    }
                    if (found) JOptionPane.showMessageDialog(null, "Movie already exists!");
                    else {
                        Movie movie = new Movie(nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                        for (int i = 0; i < directorNames.size(); i++) {
                            movie.addDirector(directorNames.get(i));
                        }
                        for (int i = 0; i < actorNames.size(); i++) {
                            movie.addActor(actorNames.get(i));
                        }
                        for (int i = 0; i < genreList.size(); i++) {
                            movie.addGenre(genreList.get(i).toString());
                        }
                        currentUser.addProductionSystem(movie);
                        JOptionPane.showMessageDialog(null, "Movie added successfully!");
                        jPanel.removeAll();
                        mainMenuComponents(jPanel, frame, currentUser);
                    }
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: ", Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        namePanel.add(nameFieldText);

        panel.add(namePanel);

        JPanel yearPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        yearPanel.setBackground(new Color(247, 246, 242));
        yearPanel.add(makeLabel("Release Year: ", Font.BOLD, 15, 81, 191, 164));

        yearPanel.add(yearSpinner);

        panel.add(yearPanel);

        JPanel lengthPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        lengthPanel.setBackground(new Color(247, 246, 242));
        lengthPanel.add(makeLabel("Movie length: ", Font.BOLD, 15, 81, 191, 164));

        lengthPanel.add(lengthText);

        panel.add(lengthPanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel buttonsToAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsToAddPanel.setBackground(new Color(247, 246, 242));

        JButton addDirector = new JButton("Add Director");
        addDirector.setBackground(new Color(81, 191, 164));
        addDirector.setPreferredSize(new Dimension(200, 40));
        addDirector.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addDirector.setForeground(Color.WHITE);
        addDirector.setFont(new Font("Arial", Font.BOLD, 15));

        addDirector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String directorName = nameField.getText();

                    directorNames.add(directorName);
                    JOptionPane.showMessageDialog(null, "Director added successfully!");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });
        JButton addActor = new JButton("Add Actor");
        addActor.setBackground(new Color(81, 191, 164));
        addActor.setPreferredSize(new Dimension(200, 40));
        addActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addActor.setForeground(Color.WHITE);
        addActor.setFont(new Font("Arial", Font.BOLD, 15));

        addActor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add an actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String actorName = nameField.getText();

                    actorNames.add(actorName);
                    JOptionPane.showMessageDialog(null, "Actor added successfully!");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });
        JButton addGenre = new JButton("Add Genre");
        addGenre.setBackground(new Color(81, 191, 164));
        addGenre.setPreferredSize(new Dimension(200, 40));
        addGenre.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addGenre.setForeground(Color.WHITE);
        addGenre.setFont(new Font("Arial", Font.BOLD, 15));

        addGenre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreField = new JComboBox<>(Genre.values());

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add genre",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreField.getSelectedItem();

                    genreList.add(genre);
                    JOptionPane.showMessageDialog(null, "Genre added successfully!");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });
        buttonsToAddPanel.add(addDirector);
        buttonsToAddPanel.add(addActor);
        buttonsToAddPanel.add(addGenre);

        panel.add(buttonsToAddPanel);

        JPanel removeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeButtonsPanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Director");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(directorNames.toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    directorNames.remove(name);
                    JOptionPane.showMessageDialog(null, "Director removed successfully");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });

        JButton removeActorButton = new JButton("Remove Actor");
        removeActorButton.setPreferredSize(new Dimension(200, 40));
        removeActorButton.setBackground(new Color(247, 246, 242));
        removeActorButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeActorButton.setForeground(new Color(81, 137, 124));
        removeActorButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeActorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(actorNames.toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    actorNames.remove(name);
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });

        JButton removeGenreButton = new JButton("Remove Genre");
        removeGenreButton.setPreferredSize(new Dimension(200, 40));
        removeGenreButton.setBackground(new Color(247, 246, 242));
        removeGenreButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeGenreButton.setForeground(new Color(81, 137, 124));
        removeGenreButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeGenreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreBox = new JComboBox<>(genreList.toArray(new Genre[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreBox.getSelectedItem();

                    genreList.remove(genre);
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    addMovieGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                }
            }
        });

        removeButtonsPanel.add(removePerformanceButton);
        removeButtonsPanel.add(removeActorButton);
        removeButtonsPanel.add(removeGenreButton);

        panel.add(removeButtonsPanel);

        if (directorNames.isEmpty()) {
            JPanel directorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            directorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea directorsText = new JTextArea("Directors: None");
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }
        else {
            JPanel directorsPanel= new JPanel(new BorderLayout());
            directorsPanel.setBackground(new Color(247, 246, 242));

            String directors = "Directors: ";
            for (int i = 0; i < directorNames.size(); i++){
                directors += directorNames.get(i);
                if (i < directorNames.size() - 1){
                    directors += ", ";
                }
            }

            JTextArea directorsText = new JTextArea(directors);
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }

        if (actorNames.isEmpty()) {
            JPanel actorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            actorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea actorsText = new JTextArea("Actors: None");
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }
        else {
            JPanel actorsPanel= new JPanel(new BorderLayout());
            actorsPanel.setBackground(new Color(247, 246, 242));

            String actorss = "Actors: ";
            for (int i = 0; i < actorNames.size(); i++){
                actorss += actorNames.get(i);
                if (i < actorNames.size() - 1){
                    actorss += ", ";
                }
            }

            JTextArea actorsText = new JTextArea(actorss);
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }

        if (genreList.isEmpty()) {
            JPanel genresPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            genresPanel.setBackground(new Color(247, 246, 242));

            JTextArea genresText = new JTextArea("Genres: None");
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        else {
            JPanel genresPanel= new JPanel(new BorderLayout());
            genresPanel.setBackground(new Color(247, 246, 242));

            String genres = "Genres: ";
            for (int i = 0; i < genreList.size(); i++){
                genres += genreList.get(i);
                if (i < genreList.size() - 1){
                    genres += ", ";
                }
            }

            JTextArea genresText = new JTextArea(genres);
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void addSeriesGUI(JPanel upperBorder, JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, List<String> directorNames, List<String> actorNames, List<Genre> genreList, String nameText, String bioText, int year, int nrSeasons, Map<String, List<Episode>> seasons){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JTextField nameFieldText = new JTextField(nameText);
        nameFieldText.setPreferredSize(new Dimension(100, 30));
        nameFieldText.setBackground(new Color(235, 230, 226));
        nameFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        nameFieldText.setForeground(Color.BLACK);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, currentYear - 100, currentYear + 100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setValue(year);

        JTextArea bioField = new JTextArea(bioText);
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (bioText.equals("Enter synopsis")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter synopsis")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter synopsis");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nrSeasons < 0 || nameFieldText.getText().isEmpty() || bioField.getText().equals("Enter synopsis")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    boolean found = false;
                    for (Series series1 : series){
                        if (series1.getTitle().equals(nameFieldText.getText())){
                            found = true;
                            break;
                        }
                    }
                    if (found) JOptionPane.showMessageDialog(null, "Series already exists!");
                    else {
                        Series series1 = new Series(nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(),nrSeasons);
                        for (int i = 0; i < directorNames.size(); i++) {
                            series1.addDirector(directorNames.get(i));
                        }
                        for (int i = 0; i < actorNames.size(); i++) {
                            series1.addActor(actorNames.get(i));
                        }
                        for (int i = 0; i < genreList.size(); i++) {
                            series1.addGenre(genreList.get(i).toString());
                        }
                        for (Map.Entry<String, List<Episode>> entry : seasons.entrySet()) {
                            series1.addSeason(entry.getKey(), entry.getValue());
                        }
                        currentUser.addProductionSystem(series1);
                        JOptionPane.showMessageDialog(null, "Series added successfully!");
                        jPanel.removeAll();
                        mainMenuComponents(jPanel, frame, currentUser);
                    }
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: ", Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        namePanel.add(nameFieldText);

        panel.add(namePanel);

        JPanel yearPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        yearPanel.setBackground(new Color(247, 246, 242));
        yearPanel.add(makeLabel("Release Year: ", Font.BOLD, 15, 81, 191, 164));

        yearPanel.add(yearSpinner);

        panel.add(yearPanel);

        JPanel lengthPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        lengthPanel.setBackground(new Color(247, 246, 242));
        lengthPanel.add(makeLabel("Number of seasons: " + nrSeasons, Font.BOLD, 15, 81, 191, 164));

        panel.add(lengthPanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel buttonsToAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsToAddPanel.setBackground(new Color(247, 246, 242));

        JButton addDirector = new JButton("Add Director");
        addDirector.setBackground(new Color(81, 191, 164));
        addDirector.setPreferredSize(new Dimension(200, 40));
        addDirector.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addDirector.setForeground(Color.WHITE);
        addDirector.setFont(new Font("Arial", Font.BOLD, 15));

        addDirector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String directorName = nameField.getText();

                    directorNames.add(directorName);
                    JOptionPane.showMessageDialog(null, "Director added successfully!");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(),nrSeasons, seasons);
                }
            }
        });
        JButton addActor = new JButton("Add Actor");
        addActor.setBackground(new Color(81, 191, 164));
        addActor.setPreferredSize(new Dimension(200, 40));
        addActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addActor.setForeground(Color.WHITE);
        addActor.setFont(new Font("Arial", Font.BOLD, 15));

        addActor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add an actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String actorName = nameField.getText();

                    actorNames.add(actorName);
                    JOptionPane.showMessageDialog(null, "Actor added successfully!");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });
        JButton addGenre = new JButton("Add Genre");
        addGenre.setBackground(new Color(81, 191, 164));
        addGenre.setPreferredSize(new Dimension(200, 40));
        addGenre.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addGenre.setForeground(Color.WHITE);
        addGenre.setFont(new Font("Arial", Font.BOLD, 15));

        addGenre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreField = new JComboBox<>(Genre.values());

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add genre",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreField.getSelectedItem();

                    genreList.add(genre);
                    JOptionPane.showMessageDialog(null, "Genre added successfully!");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });

        JButton addSeasonButton = new JButton("Add Season");
        addSeasonButton.setBackground(new Color(81, 191, 164));
        addSeasonButton.setPreferredSize(new Dimension(200, 40));
        addSeasonButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addSeasonButton.setForeground(Color.WHITE);
        addSeasonButton.setFont(new Font("Arial", Font.BOLD, 15));

        addSeasonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JLabel seasonNameLabel = new JLabel("Season Name:");
                JTextField seasonNameField = new JTextField();
                JLabel episodeLabel = new JLabel("Number of Episodes:");
                JSpinner episodeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

                panel.add(seasonNameLabel);
                panel.add(seasonNameField);
                panel.add(episodeLabel);
                panel.add(episodeSpinner);

                List<Episode> episodesList = new ArrayList<>();

                int result = JOptionPane.showConfirmDialog(null, panel, "Add Season",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    int numberOfEpisodes =(Integer) episodeSpinner.getValue();

                    JPanel episodePanel = new JPanel(new GridLayout(numberOfEpisodes, 4, 10, 10));
                    List<JTextField> episodeNameFields = new ArrayList<>();
                    List<JTextField> episodeLengthFields = new ArrayList<>();

                    for (int i = 0; i < numberOfEpisodes; i++) {
                        JLabel nameLabel = new JLabel("Episode " + (i + 1) + " Name:");
                        JTextField nameField = new JTextField();
                        episodeNameFields.add(nameField);

                        JLabel lengthLabel = new JLabel("Length:");
                        JTextField lengthField = new JTextField();
                        episodeLengthFields.add(lengthField);

                        episodePanel.add(nameLabel);
                        episodePanel.add(nameField);
                        episodePanel.add(lengthLabel);
                        episodePanel.add(lengthField);
                    }

                    int episodeResult = JOptionPane.showConfirmDialog(null, episodePanel, "Enter Episode Details",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (episodeResult == JOptionPane.OK_OPTION) {
                        for(int i = 0; i < numberOfEpisodes; i++){
                            episodesList.add(new Episode(episodeNameFields.get(i).getText(), episodeLengthFields.get(i).getText()));
                        }
                    }

                }
                seasons.put(seasonNameField.getText(), episodesList);
                JOptionPane.showMessageDialog(null, "Season added successfully");
                addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), seasons.size(), seasons);
            }
        });

        buttonsToAddPanel.add(addDirector);
        buttonsToAddPanel.add(addActor);
        buttonsToAddPanel.add(addGenre);
        buttonsToAddPanel.add(addSeasonButton);

        panel.add(buttonsToAddPanel);

        JPanel removeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeButtonsPanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Director");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(directorNames.toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    directorNames.remove(name);
                    JOptionPane.showMessageDialog(null, "Director removed successfully");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });

        JButton removeActorButton = new JButton("Remove Actor");
        removeActorButton.setPreferredSize(new Dimension(200, 40));
        removeActorButton.setBackground(new Color(247, 246, 242));
        removeActorButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeActorButton.setForeground(new Color(81, 137, 124));
        removeActorButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeActorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(actorNames.toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    actorNames.remove(name);
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });

        JButton removeGenreButton = new JButton("Remove Genre");
        removeGenreButton.setPreferredSize(new Dimension(200, 40));
        removeGenreButton.setBackground(new Color(247, 246, 242));
        removeGenreButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeGenreButton.setForeground(new Color(81, 137, 124));
        removeGenreButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeGenreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreBox = new JComboBox<>(genreList.toArray(new Genre[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreBox.getSelectedItem();

                    genreList.remove(genre);
                    JOptionPane.showMessageDialog(null, "Genre removed successfully");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });

        JButton removeSeasonButton = new JButton("Remove Season");
        removeSeasonButton.setPreferredSize(new Dimension(200, 40));
        removeSeasonButton.setBackground(new Color(247, 246, 242));
        removeSeasonButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeSeasonButton.setForeground(new Color(81, 137, 124));
        removeSeasonButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeSeasonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> seasonNameComboBox = new JComboBox<String>(seasons.keySet().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(seasonNameComboBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String seasonNameComboBoxSelectedItem = (String) seasonNameComboBox.getSelectedItem();

                    seasons.remove(seasonNameComboBoxSelectedItem);
                    JOptionPane.showMessageDialog(null, "Season removed successfully");
                    addSeriesGUI(upperBorder, jPanel, frame, scrollPane, currentUser, directorNames, actorNames, genreList, nameFieldText.getText(), bioField.getText(),(Integer) yearSpinner.getValue(), nrSeasons, seasons);
                }
            }
        });

        removeButtonsPanel.add(removePerformanceButton);
        removeButtonsPanel.add(removeActorButton);
        removeButtonsPanel.add(removeGenreButton);
        removeButtonsPanel.add(removeSeasonButton);

        panel.add(removeButtonsPanel);

        if (directorNames.isEmpty()) {
            JPanel directorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            directorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea directorsText = new JTextArea("Directors: None");
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }
        else {
            JPanel directorsPanel= new JPanel(new BorderLayout());
            directorsPanel.setBackground(new Color(247, 246, 242));

            String directors = "Directors: ";
            for (int i = 0; i < directorNames.size(); i++){
                directors += directorNames.get(i);
                if (i < directorNames.size() - 1){
                    directors += ", ";
                }
            }

            JTextArea directorsText = new JTextArea(directors);
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }

        if (actorNames.isEmpty()) {
            JPanel actorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            actorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea actorsText = new JTextArea("Actors: None");
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }
        else {
            JPanel actorsPanel= new JPanel(new BorderLayout());
            actorsPanel.setBackground(new Color(247, 246, 242));

            String actorss = "Actors: ";
            for (int i = 0; i < actorNames.size(); i++){
                actorss += actorNames.get(i);
                if (i < actorNames.size() - 1){
                    actorss += ", ";
                }
            }

            JTextArea actorsText = new JTextArea(actorss);
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }

        if (genreList.isEmpty()) {
            JPanel genresPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            genresPanel.setBackground(new Color(247, 246, 242));

            JTextArea genresText = new JTextArea("Genres: None");
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        else {
            JPanel genresPanel= new JPanel(new BorderLayout());
            genresPanel.setBackground(new Color(247, 246, 242));

            String genres = "Genres: ";
            for (int i = 0; i < genreList.size(); i++){
                genres += genreList.get(i);
                if (i < genreList.size() - 1){
                    genres += ", ";
                }
            }

            JTextArea genresText = new JTextArea(genres);
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        if (seasons.size() <= 0){
            JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            seasonsPanel.setBackground(new Color(247, 246, 242));
            seasonsPanel.add(makeLabel("No seasons added yet", Font.BOLD, 15, 81, 191, 164));
            panel.add(seasonsPanel);
        }
        else {
            for (Map.Entry<String, List<Episode>> entry : seasons.entrySet()) {
                String seasonNumber = entry.getKey();
                List<Episode> episodes = entry.getValue();

                JLabel season = makeLabel(seasonNumber + ":", Font.BOLD, 15, 81, 191, 164);
                season.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                seasonsPanel.setBackground(new Color(247, 246, 242));
                seasonsPanel.add(season);

                panel.add(seasonsPanel);
                int i = 0;
                for (Episode episode : episodes) {
                    i++;
                    JLabel ep = makeLabel("    Episode " + i + " - " + episode.getEpisodeName(), Font.BOLD, 15, 81, 191, 164);
                    ep.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel episodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    episodePanel.setBackground(new Color(247, 246, 242));
                    episodePanel.add(ep);

                    panel.add(episodePanel);
                }
            }
        }

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void updateActorGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, Actor actor){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JTextArea bioField = new JTextArea(actor.getBiography());
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (actor.getBiography().equals("Enter biography")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter biography")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter biography");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bioField.getText().equals("Enter biography")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    Actor updatedActor = new Actor(actor.getActorName(), bioField.getText());
                    for (Map.Entry<String, String> entry : actor.getPerformances().entrySet()) {
                        updatedActor.getPerformances().put(entry.getKey(), entry.getValue());
                    }
                    currentUser.updateActor(updatedActor);
                    JOptionPane.showMessageDialog(null, "Actor updated successfully!");
                    jPanel.removeAll();
                    mainMenuComponents(jPanel, frame, currentUser);
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentUser.updateActor(auxiliaryActor);
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: " + actor.getActorName(), Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        panel.add(namePanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel performancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        performancePanel.setBackground(new Color(247, 246, 242));

        JButton addPerformanceButton = new JButton("Add Performance");
        addPerformanceButton.setPreferredSize(new Dimension(200, 40));
        addPerformanceButton.setBackground(new Color(81, 191, 164));
        addPerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addPerformanceButton.setForeground(Color.WHITE);
        addPerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        addPerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();
                JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Movie", "Series"});

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Performance Name:"));
                popupPanel.add(nameField);
                popupPanel.add(new JLabel("Performance Type:"));
                popupPanel.add(typeComboBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a performance",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String performanceName = nameField.getText();
                    String performanceType = (String) typeComboBox.getSelectedItem();

                    actor.addPerformance(performanceName, performanceType);
                    JOptionPane.showMessageDialog(null, "Performance added successfully!");
                    actor.setBiography(bioField.getText());
                    updateActorGUI(jPanel, frame, scrollPane, currentUser, actor);
                }
            }
        });
        performancePanel.add(addPerformanceButton);

        panel.add(performancePanel);

        JPanel removePerformancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removePerformancePanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Performance");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actor.getPerformances().size() <= 0){
                    JOptionPane.showMessageDialog(null, "No performances added yet!.");
                }
                else {
                    JComboBox<String> nameComboBox = new JComboBox<>(actor.getPerformances().keySet().toArray(new String[0]));

                    JPanel popupPanel = new JPanel();
                    popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                    popupPanel.add(new JLabel("Performance Name:"));
                    popupPanel.add(nameComboBox);

                    int result = JOptionPane.showOptionDialog(
                            null,
                            popupPanel,
                            "Remove a performance",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            null
                    );
                    if (result == JOptionPane.OK_OPTION) {
                        String performanceName = (String) nameComboBox.getSelectedItem();
                        actor.removePerformance(performanceName);
                        JOptionPane.showMessageDialog(null, "Performance removed successfully!");
                        actor.setBiography(bioField.getText());
                        updateActorGUI(jPanel, frame, scrollPane, currentUser, actor);
                    }
                }
            }
        });
        removePerformancePanel.add(removePerformanceButton);

        panel.add(removePerformancePanel);

        if (actor.getPerformances().isEmpty()) {
            JPanel performancesPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            performancesPanel.setBackground(new Color(247, 246, 242));
            performancesPanel.add(makeLabel("Performances: None", Font.BOLD, 15, 81, 191, 164));
            panel.add(performancesPanel);
        }
        else {
            for (int i = 0; i < actor.getPerformances().size(); i++){
                JPanel performancesPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
                performancesPanel.setBackground(new Color(247, 246, 242));
                if (i == 0){
                    performancesPanel.add(makeLabel("Performances: ", Font.BOLD, 15, 81, 191, 164));
                }
                else {
                    performancesPanel.add(Box.createHorizontalStrut(110));
                }
                performancesPanel.add(makeLabel(actor.getPerformances().keySet().toArray(new String[0])[i] + " - " + actor.getPerformances().values().toArray(new String[0])[i], Font.BOLD, 15, 81, 191, 164));
                panel.add(performancesPanel);
            }
        }


        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public static void updateMovieGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, Movie movie){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, currentYear - 100, currentYear + 100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setValue(movie.getReleaseYear());

        JTextField lengthText = new JTextField(movie.getMovieLength());
        lengthText.setPreferredSize(new Dimension(100, 30));
        lengthText.setBackground(new Color(235, 230, 226));
        lengthText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        lengthText.setForeground(Color.BLACK);

        JTextArea bioField = new JTextArea(movie.getPlot());
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (movie.getPlot().equals("Enter synopsis")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter synopsis")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter synopsis");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lengthText.getText().isEmpty() || bioField.getText().equals("Enter synopsis")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    Movie updatedMovie = new Movie(movie.getTitle(), bioField.getText(),(Integer) yearSpinner.getValue(), lengthText.getText());
                    for (int i = 0; i < movie.getDirectors().size(); i++) {
                        updatedMovie.addDirector(movie.getDirectors().get(i));
                    }
                    for (int i = 0; i < movie.getActors().size(); i++) {
                        updatedMovie.addActor(movie.getActors().get(i));
                    }
                    for (int i = 0; i < movie.getGenres().size(); i++) {
                        updatedMovie.addGenre(movie.getGenres().get(i).toString());
                    }
                    currentUser.updateProduction(updatedMovie);
                    JOptionPane.showMessageDialog(null, "Movie updated successfully!");
                    jPanel.removeAll();
                    mainMenuComponents(jPanel, frame, currentUser);
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentUser.updateProduction(auxiliaryMovie);
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: ", Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        panel.add(namePanel);

        JPanel yearPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        yearPanel.setBackground(new Color(247, 246, 242));
        yearPanel.add(makeLabel("Release Year: ", Font.BOLD, 15, 81, 191, 164));

        yearPanel.add(yearSpinner);

        panel.add(yearPanel);

        JPanel lengthPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        lengthPanel.setBackground(new Color(247, 246, 242));
        lengthPanel.add(makeLabel("Movie length: ", Font.BOLD, 15, 81, 191, 164));

        lengthPanel.add(lengthText);

        panel.add(lengthPanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel buttonsToAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsToAddPanel.setBackground(new Color(247, 246, 242));

        JButton addDirector = new JButton("Add Director");
        addDirector.setBackground(new Color(81, 191, 164));
        addDirector.setPreferredSize(new Dimension(200, 40));
        addDirector.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addDirector.setForeground(Color.WHITE);
        addDirector.setFont(new Font("Arial", Font.BOLD, 15));

        addDirector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String directorName = nameField.getText();

                    movie.addDirector(directorName);
                    JOptionPane.showMessageDialog(null, "Director added successfully!");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });
        JButton addActor = new JButton("Add Actor");
        addActor.setBackground(new Color(81, 191, 164));
        addActor.setPreferredSize(new Dimension(200, 40));
        addActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addActor.setForeground(Color.WHITE);
        addActor.setFont(new Font("Arial", Font.BOLD, 15));

        addActor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add an actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String actorName = nameField.getText();

                    movie.addActor(actorName);
                    JOptionPane.showMessageDialog(null, "Actor added successfully!");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });
        JButton addGenre = new JButton("Add Genre");
        addGenre.setBackground(new Color(81, 191, 164));
        addGenre.setPreferredSize(new Dimension(200, 40));
        addGenre.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addGenre.setForeground(Color.WHITE);
        addGenre.setFont(new Font("Arial", Font.BOLD, 15));

        addGenre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreField = new JComboBox<>(Genre.values());

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add genre",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreField.getSelectedItem();

                    movie.addGenre(String.valueOf(genre));
                    JOptionPane.showMessageDialog(null, "Genre added successfully!");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });
        buttonsToAddPanel.add(addDirector);
        buttonsToAddPanel.add(addActor);
        buttonsToAddPanel.add(addGenre);

        panel.add(buttonsToAddPanel);

        JPanel removeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeButtonsPanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Director");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(movie.getDirectors().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    movie.removeDirector(name);
                    JOptionPane.showMessageDialog(null, "Director removed successfully");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });

        JButton removeActorButton = new JButton("Remove Actor");
        removeActorButton.setPreferredSize(new Dimension(200, 40));
        removeActorButton.setBackground(new Color(247, 246, 242));
        removeActorButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeActorButton.setForeground(new Color(81, 137, 124));
        removeActorButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeActorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(movie.getActors().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    movie.getActors().remove(name);
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });

        JButton removeGenreButton = new JButton("Remove Genre");
        removeGenreButton.setPreferredSize(new Dimension(200, 40));
        removeGenreButton.setBackground(new Color(247, 246, 242));
        removeGenreButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeGenreButton.setForeground(new Color(81, 137, 124));
        removeGenreButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeGenreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreBox = new JComboBox<>(movie.getGenres().toArray(new Genre[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreBox.getSelectedItem();

                    movie.removeGenre(String.valueOf(genre));
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    movie.setPlot(bioField.getText());
                    movie.setMovieLength(lengthText.getText());
                    movie.setReleaseYear((Integer) yearSpinner.getValue());
                    updateMovieGUI(jPanel, frame, scrollPane, currentUser, movie);
                }
            }
        });

        removeButtonsPanel.add(removePerformanceButton);
        removeButtonsPanel.add(removeActorButton);
        removeButtonsPanel.add(removeGenreButton);

        panel.add(removeButtonsPanel);

        if (movie.getDirectors().isEmpty()) {
            JPanel directorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            directorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea directorsText = new JTextArea("Directors: None");
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }
        else {
            JPanel directorsPanel= new JPanel(new BorderLayout());
            directorsPanel.setBackground(new Color(247, 246, 242));

            String directors = "Directors: ";
            for (int i = 0; i < movie.getDirectors().size(); i++){
                directors += movie.getDirectors().get(i);
                if (i < movie.getDirectors().size() - 1){
                    directors += ", ";
                }
            }

            JTextArea directorsText = new JTextArea(directors);
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }

        if (movie.getActors().isEmpty()) {
            JPanel actorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            actorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea actorsText = new JTextArea("Actors: None");
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }
        else {
            JPanel actorsPanel= new JPanel(new BorderLayout());
            actorsPanel.setBackground(new Color(247, 246, 242));

            String actorss = "Actors: ";
            for (int i = 0; i < movie.getActors().size(); i++){
                actorss += movie.getActors().get(i);
                if (i < movie.getActors().size() - 1){
                    actorss += ", ";
                }
            }

            JTextArea actorsText = new JTextArea(actorss);
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }

        if (movie.getGenres().isEmpty()) {
            JPanel genresPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            genresPanel.setBackground(new Color(247, 246, 242));

            JTextArea genresText = new JTextArea("Genres: None");
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        else {
            JPanel genresPanel= new JPanel(new BorderLayout());
            genresPanel.setBackground(new Color(247, 246, 242));

            String genres = "Genres: ";
            for (int i = 0; i < movie.getGenres().size(); i++){
                genres += movie.getGenres().get(i);
                if (i < movie.getGenres().size() - 1){
                    genres += ", ";
                }
            }

            JTextArea genresText = new JTextArea(genres);
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void updateSeriesGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Staff currentUser, Series currentSeries){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, currentYear - 100, currentYear + 100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setValue(currentSeries.getReleaseYear());

        JTextArea bioField = new JTextArea(currentSeries.getPlot());
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(new Color(235, 230, 226));
        bioField.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        if (currentSeries.getPlot().equals("Enter synopsis")) bioField.setForeground(Color.GRAY);
        else bioField.setForeground(Color.BLACK);
        bioField.setPreferredSize(new Dimension(300, 150));
        bioField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (bioField.getText().equals("Enter synopsis")) {
                    bioField.setText("");
                    bioField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (bioField.getText().isEmpty()) {
                    bioField.setForeground(Color.GRAY);
                    bioField.setText("Enter synopsis");
                }
            }
        });

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentSeries.getSeasonsNumber() < 0 || bioField.getText().equals("Enter synopsis")) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    Series series1 = new Series(currentSeries.getTitle(), bioField.getText(),(Integer) yearSpinner.getValue(),currentSeries.getSeasonsNumber());
                    for (int i = 0; i < currentSeries.getDirectors().size(); i++) {
                        series1.addDirector(currentSeries.getDirectors().get(i));
                    }
                    for (int i = 0; i < currentSeries.getActors().size(); i++) {
                        series1.addActor(currentSeries.getActors().get(i));
                    }
                    for (int i = 0; i < currentSeries.getGenres().size(); i++) {
                        series1.addGenre(currentSeries.getGenres().get(i).toString());
                    }
                    for (Map.Entry<String, List<Episode>> entry : currentSeries.getSeasons().entrySet()) {
                        series1.addSeason(entry.getKey(), entry.getValue());
                    }
                    currentUser.updateProduction(series1);
                    JOptionPane.showMessageDialog(null, "Series updated successfully!");
                    jPanel.removeAll();
                    mainMenuComponents(jPanel, frame, currentUser);
                }
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(finish);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentUser.updateProduction(auxiliarySeries);
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel downAlignGoBack = new JPanel();
        downAlignGoBack.setLayout(new BoxLayout(downAlignGoBack, BoxLayout.Y_AXIS));
        downAlignGoBack.setBackground(new Color(247, 246, 242));
        downAlignGoBack.add(Box.createVerticalStrut(20));
        downAlignGoBack.add(goBack);

        upperButtons.add(downAlign);
        upperButtons.add(downAlignGoBack);

        panel.add(upperButtons);

        JPanel namePanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        namePanel.setBackground(new Color(247, 246, 242));
        namePanel.add(makeLabel("Name: ", Font.BOLD, 15, 81, 191, 164));
        namePanel.add(Box.createHorizontalStrut(30));

        panel.add(namePanel);

        JPanel yearPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        yearPanel.setBackground(new Color(247, 246, 242));
        yearPanel.add(makeLabel("Release Year: ", Font.BOLD, 15, 81, 191, 164));

        yearPanel.add(yearSpinner);

        panel.add(yearPanel);

        JPanel lengthPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        lengthPanel.setBackground(new Color(247, 246, 242));
        lengthPanel.add(makeLabel("Number of seasons: " + currentSeries.getSeasonsNumber(), Font.BOLD, 15, 81, 191, 164));

        panel.add(lengthPanel);

        JPanel bioPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
        bioPanel.setBackground(new Color(247, 246, 242));

        bioPanel.add(Box.createHorizontalStrut(85));
        bioPanel.add(bioField);

        panel.add(bioPanel);

        JPanel buttonsToAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsToAddPanel.setBackground(new Color(247, 246, 242));

        JButton addDirector = new JButton("Add Director");
        addDirector.setBackground(new Color(81, 191, 164));
        addDirector.setPreferredSize(new Dimension(200, 40));
        addDirector.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addDirector.setForeground(Color.WHITE);
        addDirector.setFont(new Font("Arial", Font.BOLD, 15));

        addDirector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add a director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String directorName = nameField.getText();

                    currentSeries.addDirector(directorName);
                    JOptionPane.showMessageDialog(null, "Director added successfully!");
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });
        JButton addActor = new JButton("Add Actor");
        addActor.setBackground(new Color(81, 191, 164));
        addActor.setPreferredSize(new Dimension(200, 40));
        addActor.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addActor.setForeground(Color.WHITE);
        addActor.setFont(new Font("Arial", Font.BOLD, 15));

        addActor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField nameField = new JTextField();

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor Name:"));
                popupPanel.add(nameField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add an actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String actorName = nameField.getText();

                    currentSeries.addActor(actorName);
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    JOptionPane.showMessageDialog(null, "Actor added successfully!");
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });
        JButton addGenre = new JButton("Add Genre");
        addGenre.setBackground(new Color(81, 191, 164));
        addGenre.setPreferredSize(new Dimension(200, 40));
        addGenre.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addGenre.setForeground(Color.WHITE);
        addGenre.setFont(new Font("Arial", Font.BOLD, 15));

        addGenre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreField = new JComboBox<>(Genre.values());

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreField);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Add genre",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreField.getSelectedItem();

                    currentSeries.addGenre(String.valueOf(genre));
                    JOptionPane.showMessageDialog(null, "Genre added successfully!");
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });

        JButton addSeasonButton = new JButton("Add Season");
        addSeasonButton.setBackground(new Color(81, 191, 164));
        addSeasonButton.setPreferredSize(new Dimension(200, 40));
        addSeasonButton.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        addSeasonButton.setForeground(Color.WHITE);
        addSeasonButton.setFont(new Font("Arial", Font.BOLD, 15));

        addSeasonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JLabel seasonNameLabel = new JLabel("Season Name:");
                JTextField seasonNameField = new JTextField();
                JLabel episodeLabel = new JLabel("Number of Episodes:");
                JSpinner episodeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

                panel.add(seasonNameLabel);
                panel.add(seasonNameField);
                panel.add(episodeLabel);
                panel.add(episodeSpinner);

                List<Episode> episodesList = new ArrayList<>();

                int result = JOptionPane.showConfirmDialog(null, panel, "Add Season",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    int numberOfEpisodes =(Integer) episodeSpinner.getValue();

                    JPanel episodePanel = new JPanel(new GridLayout(numberOfEpisodes, 4, 10, 10));
                    List<JTextField> episodeNameFields = new ArrayList<>();
                    List<JTextField> episodeLengthFields = new ArrayList<>();

                    for (int i = 0; i < numberOfEpisodes; i++) {
                        JLabel nameLabel = new JLabel("Episode " + (i + 1) + " Name:");
                        JTextField nameField = new JTextField();
                        episodeNameFields.add(nameField);

                        JLabel lengthLabel = new JLabel("Length:");
                        JTextField lengthField = new JTextField();
                        episodeLengthFields.add(lengthField);

                        episodePanel.add(nameLabel);
                        episodePanel.add(nameField);
                        episodePanel.add(lengthLabel);
                        episodePanel.add(lengthField);
                    }

                    int episodeResult = JOptionPane.showConfirmDialog(null, episodePanel, "Enter Episode Details",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (episodeResult == JOptionPane.OK_OPTION) {
                        for(int i = 0; i < numberOfEpisodes; i++){
                            episodesList.add(new Episode(episodeNameFields.get(i).getText(), episodeLengthFields.get(i).getText()));
                        }
                    }

                }
                currentSeries.addSeason(seasonNameField.getText(), episodesList);
                JOptionPane.showMessageDialog(null, "Season added successfully");
                updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
            }
        });

        buttonsToAddPanel.add(addDirector);
        buttonsToAddPanel.add(addActor);
        buttonsToAddPanel.add(addGenre);
        buttonsToAddPanel.add(addSeasonButton);

        panel.add(buttonsToAddPanel);

        JPanel removeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeButtonsPanel.setBackground(new Color(247, 246, 242));

        JButton removePerformanceButton = new JButton("Remove Director");
        removePerformanceButton.setPreferredSize(new Dimension(200, 40));
        removePerformanceButton.setBackground(new Color(247, 246, 242));
        removePerformanceButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removePerformanceButton.setForeground(new Color(81, 137, 124));
        removePerformanceButton.setFont(new Font("Arial", Font.BOLD, 15));

        removePerformanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(currentSeries.getDirectors().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Director:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove director",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    currentSeries.removeDirector(name);
                    JOptionPane.showMessageDialog(null, "Director removed successfully");
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });

        JButton removeActorButton = new JButton("Remove Actor");
        removeActorButton.setPreferredSize(new Dimension(200, 40));
        removeActorButton.setBackground(new Color(247, 246, 242));
        removeActorButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeActorButton.setForeground(new Color(81, 137, 124));
        removeActorButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeActorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nameBox = new JComboBox<>(currentSeries.getActors().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Actor:"));
                popupPanel.add(nameBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String name = (String) nameBox.getSelectedItem();

                    currentSeries.getActors().remove(name);
                    JOptionPane.showMessageDialog(null, "Actor removed successfully");
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });

        JButton removeGenreButton = new JButton("Remove Genre");
        removeGenreButton.setPreferredSize(new Dimension(200, 40));
        removeGenreButton.setBackground(new Color(247, 246, 242));
        removeGenreButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeGenreButton.setForeground(new Color(81, 137, 124));
        removeGenreButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeGenreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<Genre> genreBox = new JComboBox<>(currentSeries.getGenres().toArray(new Genre[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(genreBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    Genre genre = (Genre) genreBox.getSelectedItem();

                    currentSeries.removeGenre(String.valueOf(genre));
                    JOptionPane.showMessageDialog(null, "Genre removed successfully");
                    currentSeries.setPlot(bioField.getText());
                    currentSeries.setReleaseYear((Integer) yearSpinner.getValue());
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });

        JButton removeSeasonButton = new JButton("Remove Season");
        removeSeasonButton.setPreferredSize(new Dimension(200, 40));
        removeSeasonButton.setBackground(new Color(247, 246, 242));
        removeSeasonButton.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124)));
        removeSeasonButton.setForeground(new Color(81, 137, 124));
        removeSeasonButton.setFont(new Font("Arial", Font.BOLD, 15));

        removeSeasonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> seasonNameComboBox = new JComboBox<String>(currentSeries.getSeasons().keySet().toArray(new String[0]));

                JPanel popupPanel = new JPanel();
                popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
                popupPanel.add(new JLabel("Genre:"));
                popupPanel.add(seasonNameComboBox);

                int result = JOptionPane.showOptionDialog(
                        null,
                        popupPanel,
                        "Remove actor",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if (result == JOptionPane.OK_OPTION) {
                    String seasonNameComboBoxSelectedItem = (String) seasonNameComboBox.getSelectedItem();

                    currentSeries.getSeasons().remove(seasonNameComboBoxSelectedItem);
                    JOptionPane.showMessageDialog(null, "Season removed successfully");
                    updateSeriesGUI(jPanel, frame, scrollPane, currentUser, currentSeries);
                }
            }
        });

        removeButtonsPanel.add(removePerformanceButton);
        removeButtonsPanel.add(removeActorButton);
        removeButtonsPanel.add(removeGenreButton);
        removeButtonsPanel.add(removeSeasonButton);

        panel.add(removeButtonsPanel);

        if (currentSeries.getDirectors().isEmpty()) {
            JPanel directorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            directorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea directorsText = new JTextArea("Directors: None");
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }
        else {
            JPanel directorsPanel= new JPanel(new BorderLayout());
            directorsPanel.setBackground(new Color(247, 246, 242));

            String directors = "Directors: ";
            for (int i = 0; i < currentSeries.getDirectors().size(); i++){
                directors += currentSeries.getDirectors().get(i);
                if (i < currentSeries.getDirectors().size() - 1){
                    directors += ", ";
                }
            }

            JTextArea directorsText = new JTextArea(directors);
            directorsText.setBackground(new Color(247, 246, 242));
            directorsText.setPreferredSize(new Dimension(500, 30));
            directorsText.setForeground(Color.BLACK);
            directorsText.setFont(new Font("Arial", Font.BOLD, 15));
            directorsText.setEditable(false);
            directorsText.setWrapStyleWord(true);
            directorsText.setLineWrap(true);
            directorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            directorsPanel.add(directorsText);

            panel.add(directorsPanel);
        }

        if (currentSeries.getActors().isEmpty()) {
            JPanel actorsPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            actorsPanel.setBackground(new Color(247, 246, 242));

            JTextArea actorsText = new JTextArea("Actors: None");
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }
        else {
            JPanel actorsPanel= new JPanel(new BorderLayout());
            actorsPanel.setBackground(new Color(247, 246, 242));

            String actorss = "Actors: ";
            for (int i = 0; i < currentSeries.getActors().size(); i++){
                actorss += currentSeries.getActors().get(i);
                if (i < currentSeries.getActors().size() - 1){
                    actorss += ", ";
                }
            }

            JTextArea actorsText = new JTextArea(actorss);
            actorsText.setBackground(new Color(247, 246, 242));
            actorsText.setPreferredSize(new Dimension(500, 30));
            actorsText.setForeground(Color.BLACK);
            actorsText.setFont(new Font("Arial", Font.BOLD, 15));
            actorsText.setEditable(false);
            actorsText.setWrapStyleWord(true);
            actorsText.setLineWrap(true);
            actorsText.setAlignmentX(Component.LEFT_ALIGNMENT);

            actorsPanel.add(actorsText);

            panel.add(actorsPanel);
        }

        if (currentSeries.getGenres().isEmpty()) {
            JPanel genresPanel = new JPanel(new FlowLayout((FlowLayout.LEFT)));
            genresPanel.setBackground(new Color(247, 246, 242));

            JTextArea genresText = new JTextArea("Genres: None");
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        else {
            JPanel genresPanel= new JPanel(new BorderLayout());
            genresPanel.setBackground(new Color(247, 246, 242));

            String genres = "Genres: ";
            for (int i = 0; i < currentSeries.getGenres().size(); i++){
                genres += currentSeries.getGenres().get(i);
                if (i < currentSeries.getGenres().size() - 1){
                    genres += ", ";
                }
            }

            JTextArea genresText = new JTextArea(genres);
            genresText.setBackground(new Color(247, 246, 242));
            genresText.setPreferredSize(new Dimension(500, 30));
            genresText.setForeground(Color.BLACK);
            genresText.setFont(new Font("Arial", Font.BOLD, 15));
            genresText.setEditable(false);
            genresText.setWrapStyleWord(true);
            genresText.setLineWrap(true);
            genresText.setAlignmentX(Component.LEFT_ALIGNMENT);

            genresPanel.add(genresText);

            panel.add(genresPanel);
        }
        if (currentSeries.getSeasons().size() <= 0){
            JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            seasonsPanel.setBackground(new Color(247, 246, 242));
            seasonsPanel.add(makeLabel("No seasons added yet", Font.BOLD, 15, 81, 191, 164));
            panel.add(seasonsPanel);
        }
        else {
            for (Map.Entry<String, List<Episode>> entry : currentSeries.getSeasons().entrySet()) {
                String seasonNumber = entry.getKey();
                List<Episode> episodes = entry.getValue();

                JLabel season = makeLabel(seasonNumber + ":", Font.BOLD, 15, 81, 191, 164);
                season.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel seasonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                seasonsPanel.setBackground(new Color(247, 246, 242));
                seasonsPanel.add(season);

                panel.add(seasonsPanel);
                int i = 0;
                for (Episode episode : episodes) {
                    i++;
                    JLabel ep = makeLabel("    Episode " + i + " - " + episode.getEpisodeName(), Font.BOLD, 15, 81, 191, 164);
                    ep.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel episodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    episodePanel.setBackground(new Color(247, 246, 242));
                    episodePanel.add(ep);

                    panel.add(episodePanel);
                }
            }
        }

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    public static void addDeleteUserGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Admin currentUser){
        String[] options = {"Add User", "Delete User"};

        JComboBox<String> comboBox = new JComboBox<>(options);

        String[] userNames = new String[regularList.size() + contributorList.size() + adminList.size()];
        int i = 0;
        for (Regular regular : regularList){
            userNames[i++] = regular.getUsername();
        }
        for (Contributor contributor : contributorList){
            userNames[i++] = contributor.getUsername();
        }
        for (Admin admin : adminList){
            userNames[i++] = admin.getUsername();
        }

        JComboBox<String> usernames = new JComboBox<>(userNames);
        usernames.setVisible(false);

        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new GridLayout(0, 1)); // 2 rows, 1 column
        comboPanel.add(comboBox);
        comboPanel.add(usernames);

        comboBox.addActionListener(e -> {
            usernames.setVisible(comboBox.getSelectedItem().equals(options[1]));
        });

        int result = JOptionPane.showOptionDialog(
                null,
                comboPanel,
                "Choose what to do",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        );
        if (result == JOptionPane.OK_OPTION) {
            String selection = (String) comboBox.getSelectedItem();

            if ("Add User".equals(selection)) {
                addUserGUI(jPanel, frame, scrollPane, currentUser);
            } else if ("Delete User".equals(selection)) {
                String selectedUser = (String) usernames.getSelectedItem();

                if (selectedUser == null) {
                    JOptionPane.showMessageDialog(null, "Please select a user to delete.");
                } else if (selectedUser.equals(currentUser.getUsername())) {
                    JOptionPane.showMessageDialog(null, "You can't remove yourself!");
                } else {
                    currentUser.removeUser(selectedUser);
                    JOptionPane.showMessageDialog(null, "User removed successfully");
                    jPanel.removeAll();
                    mainMenuComponents(jPanel, frame, currentUser);
                }
            }
        }
    }
    private static String[] getNumericArray(int start, int end) {
        String[] array = new String[end - start + 1];
        for (int i = start; i <= end; i++) {
            array[i - start] = String.valueOf(i);
        }
        return array;
    }
    public static void addUserGUI(JPanel jPanel, JFrame frame, JPanel scrollPane, Admin currentUser){
        scrollPane.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(247, 246, 242));
        panel.setBounds(0, 0, 1065, 615);

        JButton goBack = new JButton("   Go Back   ");
        goBack.setBackground(new Color(81, 191, 164));
        goBack.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        goBack.setForeground(Color.WHITE);
        goBack.setFont(new Font("Arial", Font.BOLD, 15));
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jPanel.removeAll();
                mainMenuComponents(jPanel, frame, currentUser);
            }
        });

        JPanel upperButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperButtons.setBackground(new Color(247, 246, 242));

        upperButtons.add(Box.createHorizontalStrut(800));

        JPanel downAlign = new JPanel();
        downAlign.setLayout(new BoxLayout(downAlign, BoxLayout.Y_AXIS));
        downAlign.setBackground(new Color(247, 246, 242));
        downAlign.add(Box.createVerticalStrut(20));
        downAlign.add(goBack);

        upperButtons.add(downAlign);

        JTextField emailFieldText = new JTextField("");
        emailFieldText.setPreferredSize(new Dimension(300, 30));
        emailFieldText.setBackground(new Color(235, 230, 226));
        emailFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        emailFieldText.setForeground(Color.BLACK);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(8, 4, 20, 1);
        JSpinner passwordLengthSpinner = new JSpinner(spinnerModel);
        passwordLengthSpinner.setPreferredSize(new Dimension(50, 30));

        JTextField firstNameFieldText = new JTextField("");
        firstNameFieldText.setPreferredSize(new Dimension(100, 30));
        firstNameFieldText.setBackground(new Color(235, 230, 226));
        firstNameFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        firstNameFieldText.setForeground(Color.BLACK);

        JTextField lastNameFieldText = new JTextField("");
        lastNameFieldText.setPreferredSize(new Dimension(100, 30));
        lastNameFieldText.setBackground(new Color(235, 230, 226));
        lastNameFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        lastNameFieldText.setForeground(Color.BLACK);

        JTextField countryFieldText = new JTextField("");
        countryFieldText.setPreferredSize(new Dimension(100, 30));
        countryFieldText.setBackground(new Color(235, 230, 226));
        countryFieldText.setBorder(BorderFactory.createLineBorder(new Color(81, 137, 124), 4));
        countryFieldText.setForeground(Color.BLACK);

        String[] genders = {"N", "M", "F"};
        JComboBox<String> genderComboBox = new JComboBox<>(genders);

        JComboBox<String> dayComboBox = new JComboBox<>(getNumericArray(1, 31));

        String[] months = {"January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};
        JComboBox<String> monthComboBox = new JComboBox<>(months);

        JComboBox<String> yearComboBox = new JComboBox<>(getNumericArray(1900, 2022));

        String[] types = {"Regular", "Contributor", "Admin"};
        JComboBox<String> typeComboBox = new JComboBox<>(types);

        JButton finish = new JButton("   Finish   ");
        finish.setBackground(new Color(81, 191, 164));
        finish.setBorder(BorderFactory.createLineBorder(new Color(81, 191, 164)));
        finish.setForeground(Color.WHITE);
        finish.setFont(new Font("Arial", Font.BOLD, 15));
        finish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (emailFieldText.getText().isEmpty() || firstNameFieldText.getText().isEmpty() || lastNameFieldText.getText().isEmpty() || countryFieldText.getText().isEmpty()) JOptionPane.showMessageDialog(null, "Please enter valid information");
                else {
                    boolean found = false;
                    String username = User.generateUsername(firstNameFieldText.getText(), lastNameFieldText.getText());
                    for (User user : regularList){
                        if (user.getUsername().equals(username)){
                            found = true;
                            break;
                        }
                    }
                    for (User user : contributorList){
                        if (user.getUsername().equals(username)){
                            found = true;
                            break;
                        }
                    }
                    for (User user : adminList){
                        if (user.getUsername().equals(username)){
                            found = true;
                            break;
                        }
                    }
                    if (found) JOptionPane.showMessageDialog(null, "User already exists!");
                    else {
                        int day = Integer.parseInt((String) dayComboBox.getSelectedItem());
                        int month = Arrays.asList(months).indexOf(monthComboBox.getSelectedItem()) + 1;
                        int year = Integer.parseInt((String) yearComboBox.getSelectedItem());

                        String password = User.generatePassword((Integer) passwordLengthSpinner.getValue());

                        LocalDate selectedDate = LocalDate.of(year, month, day);
                        User.Information information = User.Information.builder()
                                .credentials(User.Information.credentialBuilder()
                                        .email(emailFieldText.getText())
                                        .password(password)
                                        .build())
                                .name(firstNameFieldText + " " + lastNameFieldText)
                                .country(countryFieldText.getText())
                                .age(Period.between(selectedDate, LocalDate.now()).getYears())
                                .gender(genderComboBox.getSelectedItem().toString())
                                .birthDate(selectedDate)
                                .build();

                        User newUser = User.userFactory.createUser(typeComboBox.getSelectedItem().toString(), information, username, 0);

                        switch (newUser.getType().toString().toUpperCase()) {
                            case "REGULAR":
                                for (Production production : movies) {
                                    production.addRegularsToReviews((Regular) newUser);
                                }
                                for (Production production : series) {
                                    production.addRegularsToReviews((Regular) newUser);
                                }
                                regularList.add((Regular) newUser);
                                break;
                            case "CONTRIBUTOR":
                                contributorList.add((Contributor) newUser);
                                break;
                            case "ADMIN":
                                adminList.add((Admin) newUser);
                                break;
                        }

                        JOptionPane.showMessageDialog(null, "User '" + newUser.getUsername() + "' with password '" + password + "' added successfully!");
                        jPanel.removeAll();
                        mainMenuComponents(jPanel, frame, currentUser);
                    }
                }
            }
        });

        JPanel downAlignFinish = new JPanel();
        downAlignFinish.setLayout(new BoxLayout(downAlignFinish, BoxLayout.Y_AXIS));
        downAlignFinish.setBackground(new Color(247, 246, 242));
        downAlignFinish.add(Box.createVerticalStrut(20));
        downAlignFinish.add(finish);

        upperButtons.add(downAlignFinish);

        panel.add(upperButtons);

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstRow.setBackground(new Color(247, 246, 242));
        firstRow.add(makeLabel("Email:", Font.BOLD,15, 81, 191, 164));
        firstRow.add(emailFieldText);
        firstRow.add(makeLabel("Password Length:", Font.BOLD,15, 81, 191, 164));
        firstRow.add(passwordLengthSpinner);

        panel.add(firstRow);

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        secondRow.setBackground(new Color(247, 246, 242));
        secondRow.add(makeLabel("First Name:", Font.BOLD,15, 81, 191, 164));
        secondRow.add(firstNameFieldText);
        secondRow.add(makeLabel("Last Name:", Font.BOLD,15, 81, 191, 164));
        secondRow.add(lastNameFieldText);
        secondRow.add(makeLabel("Type:", Font.BOLD,15, 81, 191, 164));
        secondRow.add(typeComboBox);

        panel.add(secondRow);

        JPanel thirdRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        thirdRow.setBackground(new Color(247, 246, 242));
        thirdRow.add(makeLabel("Country:", Font.BOLD,15, 81, 191, 164));
        thirdRow.add(countryFieldText);
        thirdRow.add(makeLabel("Gender:", Font.BOLD,15, 81, 191, 164));
        thirdRow.add(genderComboBox);
        thirdRow.add(makeLabel("Birth Date:", Font.BOLD,15, 81, 191, 164));
        thirdRow.add(dayComboBox);
        thirdRow.add(monthComboBox);
        thirdRow.add(yearComboBox);

        panel.add(thirdRow);

        scrollPane.add(panel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }
}
