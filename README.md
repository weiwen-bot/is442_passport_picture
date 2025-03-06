# Passport Photo Maker
List Functions

## Setup
### Prerequisites:
- Github Setup `git clone https://github.com/weiwen-bot/is442_passport_picture.git`
    - Or use Github Desktop
- [Java](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) v21
- [Node.js](https://nodejs.org/en/) v20.15.0 (_or above_)
- VSC Extensions to download (Recommended)
    - [JavaExtensionPack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
    - [SpringBootExtensionPack](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)
- I have attached the opencv_java4100.dll inside the passportphoto directory you will just have to put the absolute path in the env
- create .env file in the `backend` directory to store path for dll. (_replace the values in `<>` to with your own values):
    ```
    OPENCVDLLPATH=<>\\opencv_java4100.dll
    ```
- I also attach a simple postman collection to run the background removal
    - Ensure its a post request and select file and update a file you have (It may take awhile)

### Google Picker & Drive API Setup
Follow these steps to enable **Google Picker API** and **Google Drive API** and obtain the required credentials.

#### 1. Enable APIs
- Go to [Google Cloud Console](https://console.cloud.google.com/).
- Create or select a project.
- Navigate to **APIs & Services > Library**.
- Enable **Google Picker API** and **Google Drive API**.

#### 2. Get OAuth Client ID
- Go to **APIs & Services > Credentials**.
- Click **Create Credentials > OAuth Client ID**.
- If prompted, set up the **OAuth Consent Screen**.
- Select **Web Application** and add **Authorized JavaScript origins**:
  - Example for local development: `http://localhost:3000`
  - Example for production: `https://yourdomain.com`
- Click **Create** and copy the **Client ID**.

#### 3. Get API Key
- Go to **APIs & Services > Credentials**.
- Click **Create Credentials > API Key** and copy it.

#### 4. Store Credentials in Environment File
- In your `frontend` directory, create or update the `.env` file:
    ```
    VITE_GOOGLE_CLIENT_ID=<your-client-id-here>
    VITE_GOOGLE_API_KEY=<your-api-key-here>
    ```

### Information about the java directory
[ReadThisArticle](https://malshani-wijekoon.medium.com/spring-boot-folder-structure-best-practices-18ef78a81819) 

### Github Procedures
- Create a Branch from main for you own self.
- Once done with task send Pull Request to main if no conflicts can merge else fix the conflict (Please ask the other person if unsure)


### Running the app:
1. Cd Backend/passportphoto

    `~/backend/passportphoto$ run.bat`

    For Mac users, execute the `chmod +x mvnw` then `source run.sh` 

1. Install dependencies in the `frontend` directory.

    `~/frontend$ npm install`

1. Run the frontend server. It will be on http://localhost:5173 by default.

    `~/frontend$ npm run dev`
