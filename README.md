# Passport Photo Maker

## List of Functions
Pre-Processing
- Upload from local device or Google Drive
- Image cropping & resizing

Image Processing
- Background removal
- Background colour/photo replacement
- Photo enhancement

Post-Processing
- Download to local device or Google Drive
- Export multiple layout options

Other Tools
- Country-specific size & colour presets
- Undo / Redo
- Batch processing
- Face centering

## Setup

### Prerequisites:

- Clone the repository: 
    `git clone https://github.com/weiwen-bot/is442_passport_picture.git`
    _Or use Github Desktop_
- [Java](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) v21
- [Node.js](https://nodejs.org/en/) v20.15.0 (_or above_)
- Recommended VSC Extensions:
    - [JavaExtensionPack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
    - [SpringBootExtensionPack](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)

---

### Backend Configuration

- The required OpenCV library is already included in the `passportphoto` directory:
    - **Windows**: `opencv_java4100.dll`
    - **macOS**: `libopencv_java480.dylib`
- Create a `.env` file in the `backend/passportphoto` directory and add the absolute path to the correct OpenCV library:
    ```env
    # Windows
    OPENCVDLLPATH=<absolute-path>\\opencv_java4100.dll

    # macOS
    OPENCVDLLPATH=<absolute-path>/libopencv_java480.dylib
    ```

---

### Background Removal Testing

A Postman collection is included for testing background removal.  
To test:
- Use a POST request.
- Upload any image file.
- Wait for the processed image to return (may take a few seconds).

---

### Google Picker & Drive API Setup

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
  - Example for local development: `http://localhost:5173`
- Click **Create** and copy the **Client ID**.

#### 3. Add Test Users
- Go to APIs & Services > OAuth consent screen > Audience.
- In the Test users section, click Add users.
- Add the email address(es) of testers who will access the app during development.

#### 4. Get API Key
- Go to **APIs & Services > Credentials**.
- Click **Create Credentials > API Key** and copy it.

#### 5. Store Credentials in Environment File
- In your `frontend` directory, create or update the `.env` file:
    ```
    VITE_GOOGLE_CLIENT_ID=<your-client-id-here>
    VITE_GOOGLE_API_KEY=<your-api-key-here>
    ```

---

### Java Directory Structure
Refer to this [Spring Boot Folder Structure Guide](https://malshani-wijekoon.medium.com/spring-boot-folder-structure-best-practices-18ef78a81819).

---

### GitHub Workflow

- Create a branch from `main` for your changes.
- Make your edits and push to your branch.
- Open a pull request and resolve any merge conflicts.

---

### Running the app:

1. **Backend**

    Navigate to the backend directory:
    
    `cd backend/passportphoto`

    - On **Windows** Run:
    
      `run.bat`
    - On **Mac/Linux** Run:
    
      `chmod +x mvnw`
      
      `source run.sh`

2. **Frontend**

    Navigate to the backend directory:
    
    `cd frontend`

    Install dependencies:
    
    `npm install`

    Start the development server (default: [http://localhost:5173](http://localhost:5173)):
    
    `npm run dev`