# AI Interview Practice: Gemini Setup Guide

This guide records the setup we completed together, up to receiving our first AI-generated interview question. It does not contain any secret keys or passwords.

## 1. What are we building?

We are building an app for practising job interviews.

The user enters a job role and years of experience. The app asks an interview question suited to those details.

Later, the user will speak an answer. The app will turn the recording into text, evaluate the answer, ask useful follow-up questions, and produce a feedback report.

For now, the working feature is: **enter role and experience → receive one AI-generated question**.

## 2. What is an LLM?

LLM means **Large Language Model**. It is an AI model trained to work with language.

Think of it as a helper that can read instructions and write a response. In our app, it generates interview questions. Later, it will also help with feedback and follow-ups.

It can make mistakes. Its feedback will be practice guidance, not a guaranteed judgment of someone's ability.

## 3. Which AI are we using?

| Item | Our choice |
|---|---|
| Provider | Google |
| Model family | Gemini |
| API | Gemini Developer API |
| Model ID configured in our app | `gemini-3.1-flash-lite` |
| Intended billing tier | Free tier |
| Backend | Java and Spring Boot |
| Java library | Google Gen AI Java SDK |
| Library version we added | `1.73.0` |

The **provider** runs the service. The **model** is the particular AI we ask to generate a response. The **SDK** is a Java library that makes calling the API easier.

We first discussed OpenAI. We changed to Gemini because you wanted to keep development close to free. The current integration calls Gemini, not the OpenAI API or Groq.

Google listed this model with free-tier input and output when we checked during setup. Free usage has quotas and is not unlimited. Availability and limits can change. Keep the project on the Free tier and check its current limits in Google AI Studio. Creating a key alone does not confirm the project's billing tier.

Google's pricing page states that free-tier data may be used to improve its products. Use sample resume information during development.

## 4. What is an API, and why do we need a key?

An **API** is a way for one application to communicate with another service.

Your Spring Boot app sends instructions to Google's Gemini API over the internet. Gemini returns generated text.

An **API key** is a private credential, like an access pass. Google uses it to identify the project making a request and apply that project's permissions and usage limits.

It is not a prompt, model name, or password for your app's users.

## 5. How we created the key

1. Opened Google AI Studio's API keys page: https://aistudio.google.com/apikey
2. Signed in with a Google account.
3. Accepted the terms if prompted.
4. Selected a project or used the default project provided by AI Studio.
5. Created an API key and copied it privately.
6. Planned to use a project on the Free tier, without enabling paid billing.

New users may receive a default project and key automatically. The buttons can differ depending on the account.

During setup, a screenshot exposed a key. We advised deleting that exposed key and replacing it. This guide does not verify whether it was deleted: if it is still active, revoke it and use a replacement.

Never paste a key into chat, screenshots, Git commits, frontend code, or a shared document.

## 6. Where we put the key in IntelliJ

We used an **environment variable**. This is a named value supplied to the application when it starts.

In IntelliJ:

1. Open **Run → Edit Configurations**.
2. Select **InterviewServiceApplication**.
3. Find **Environment variables**. If hidden, enable it through **Modify options**.
4. Click the small list/edit icon beside the field.
5. Under **User environment variables**, click **+**.
6. Enter `GEMINI_API_KEY` in the **Name** column.
7. Paste the replacement key into the **Value** column, without quotes.
8. Keep **Include system environment variables** checked.
9. Keep any existing variables, such as `DB_PASSWORD`, if the application uses them.
10. Click **OK**, then **Apply/OK** to save.

We left **Store as project file** unchecked. Environment variables are not a magical encrypted vault: keep local IDE settings containing secrets out of Git too.

These settings apply when you launch this run configuration in IntelliJ. Running the app from another terminal or later from Docker requires supplying the environment variable there separately.

## 7. What we added to application.properties

File: `interview-service/src/main/resources/application.properties`

```properties
gemini.api.key=${GEMINI_API_KEY}
gemini.model=gemini-3.1-flash-lite
```

| Setting | Simple meaning |
|---|---|
| `gemini.api.key` | A configuration name our Java code reads |
| `${GEMINI_API_KEY}` | Ask Spring to get the actual value from the environment |
| `gemini.model` | The model we want Gemini to use |

Keep `${GEMINI_API_KEY}` exactly as written. Do not replace it with the real secret.

We removed the earlier empty `GEMINI_API_KEY=` line from this file. That line alone would not configure IntelliJ's environment.

The connection is: **IntelliJ supplies the secret → Spring resolves the placeholder → our Gemini service reads the setting**.

## 8. The Java library we added

Inside the existing `<dependencies>` section of `pom.xml`, we added:

```xml
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>1.73.0</version>
</dependency>
```

Then we saved the file and reloaded Maven in IntelliJ.

Maven downloads the library. Adding the library does not itself call Gemini or consume Gemini quota.

## 9. How our Java classes work together

| Class | Responsibility |
|---|---|
| `StartInterviewRequest` | Holds `jobRole` and `experienceYears` from the request |
| `InterviewController` | Receives the POST request and calls `GeminiService` |
| `GeminiService` | Builds the instructions, calls Gemini, and returns question text |
| `StartInterviewResponse` | Holds the question sent back to the caller |

We added validation to the request: the job role must not be blank, and experience must be between 0 and 60 whole years. Our current primitive `int` field defaults to 0 if omitted; we have not yet made the experience field explicitly required.

In `GeminiService`, `@Value` reads the key and model settings. The constructor builds Google's `Client` with the API key.

The service builds a **prompt**: instructions asking for exactly one opening question suitable for the supplied role and experience. It asks Gemini to return only the question, without an answer or greeting.

The call `client.models.generateContent(model, prompt, null)` sends the request. The `null` means we have not supplied extra generation settings.

The method `response.text()` reads the generated text. We check that it is not empty before returning it.

The controller receives `GeminiService` through its constructor. Spring supplies this object automatically; this is called **constructor injection**.

## 10. How we tested it

We restarted the app from IntelliJ after saving the configuration.

In Postman:

1. Selected **POST**.
2. Entered `http://localhost:8080/api/interviews`.
3. Selected **Body → raw → JSON**.
4. Sent:

```json
{
  "jobRole": "Java Developer",
  "experienceYears": 2
}
```

You reported this successful response:

```json
{
  "question": "Can you describe a challenging technical problem you encountered in a recent Java project and the specific steps you took to resolve it?"
}
```

This confirmed that our request travelled through Spring Boot to Gemini and that the generated text came back successfully. The wording can change between requests.

Opening the same URL in a browser's address bar sends a **GET**, not the **POST** this endpoint expects. Use Postman for this test.

## 11. The complete working flow

```text
Postman sends role and experience
          ↓
InterviewController receives the request
          ↓
Spring checks the validation rules
          ↓
GeminiService prepares the prompt
          ↓
Google's Java client sends it to Gemini using the API key
          ↓
Gemini generates one question
          ↓
Spring Boot returns the question as JSON
```

## 12. What we have not built yet

We have not yet implemented:

- Interview IDs and temporary interview storage.
- Remembering questions and answers across requests.
- Answer submission, scoring, and smart follow-ups.
- Microphone recording or Whisper transcription.
- Text-to-speech playback.
- Resume processing and final reports.
- Login and permanent interview history in PostgreSQL.
- The frontend, separate AI/report services, RabbitMQ, or Docker packaging.

PostgreSQL was set up earlier, but this question-generation feature does not save interview records to it. Our next proposed step was temporary interview storage in Java memory. We paused before that change to write this guide.

You write the backend and database code. I explain the steps and review problems. I will handle the frontend when we reach that stage.

## 13. If something goes wrong

| Problem | First thing to check |
|---|---|
| Spring cannot resolve `GEMINI_API_KEY` | The variable is present in the exact IntelliJ run configuration you started |
| Key rejected or access denied | The key is valid, belongs to the intended project, and has the required access |
| Model unavailable | The configured model ID and availability for your project |
| `429` or quota exhausted | Your project's model-specific limits; wait for the relevant quota window to reset |
| Browser does not show a question at the POST URL | Send a POST with JSON using Postman |
| Changed run settings are not taking effect | Stop and restart the app using the updated run configuration |

When asking for help, share the error message with keys and passwords removed.

## Official references

- [Create and use Gemini API keys](https://ai.google.dev/gemini-api/docs/api-key)
- [Google AI Studio API keys](https://aistudio.google.com/apikey)
- [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Gemini API rate limits](https://ai.google.dev/gemini-api/docs/rate-limits)
- [Google Gen AI Java SDK](https://github.com/googleapis/java-genai)
- [Java SDK version used in this project](https://central.sonatype.com/artifact/com.google.genai/google-genai/1.73.0)

Setup recorded on 2 October 2026. Model availability, pricing, and UI labels may change.
