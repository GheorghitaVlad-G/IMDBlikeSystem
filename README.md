# Movie Database Application
---

### **Project Overview**
This project is a movie database application implemented in Java. The goal was to design a system that allows users to interact with a movie database, providing both Command Line Interface (CLI) and Graphical User Interface (GUI) options. It was developed as part of a university assignment.

The application follows the given requirements as closely as possible, though some assumptions were made where details were missing in the provided specifications. For example, the specification did not mention that each user has a list of requests they have created. As a result, every time I display the requests of a user, I search through all the lists for admins, contributors, and request holders.

### **Design Patterns Used**
- **Singleton:** Used for the main `IMDB` class to ensure only one instance is available.
- **Builder:** Applied to `Information` and `Credentials` classes to ensure proper construction.
- **Factory:** Used for creating user instances, encapsulating the instantiation logic.
- **Observer:** Implemented to manage notifications and updates.
- **Strategy:** Used to calculate and add experience points for users based on their activities.

### **Error Handling**
Errors are handled thoroughly with helpful messages and pop-ups where applicable, guiding the user on what went wrong.

### **CLI and GUI Implementation**
- **CLI:** The Command Line Interface uses multiple print statements to interact with the user, excluding the pop-up that appears upon application startup.
- **GUI:** The Graphical User Interface is built using Swing, with all layouts and components manually coded (no UI designer used). The GUI mirrors the functionality of the CLI, allowing users to modify both personal and public data.

---

### **Features**
- Users can modify personal information such as their list of favorite movies/TV shows, experience points, etc.
- Users can interact with the application to edit public data, such as production and actor information, or create requests to higher-permission users.
- Both CLI and GUI versions provide the same set of functionalities for user interaction.

### **Usage Instructions**

#### **Dependencies**
- The application requires the **`org.json`** library.
- To use this library, you need to include the JSON file found in the `/libs` folder into your project in the IDE.

#### **Running the Application**

- **In IDE (IntelliJ IDEA):**
  1. Make sure to add the **`org.json`** library from the `/libs` folder to your project.
  2. In the IDE, run the `IMDB` class as a Java application.


### **Known Issues**
- The code is somewhat disorganized and lacks sufficient comments. Many code blocks (especially in the `IMDB` class) are copied and pasted with minor adjustments. For better organization, methods can be refactored with necessary arguments to avoid duplication.
- Some UI bugs (infinite scroll, etc.) need further resolution.

### **Future Improvements**
- **Saving Data:** Implement saving changes in JSON files, simulating a centralized database.
- **Actor Rankings:** Add rankings for actors based on user contributions or ratings.
- **UI Improvements:** Work on visual enhancements for the graphical interface. Resolve visual bugs like infinite scrolling.

---

### **Conclusion**
This project showcases an application with both CLI and GUI implementations, leveraging various design patterns for maintainability and scalability. While the code is not perfect, the core functionality is present, and further improvements can be made to both the backend (saving data) and frontend (UI/UX).
