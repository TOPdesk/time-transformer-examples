Example usage of the [`TimeTransformer`](https://github.com/TOPdesk/time-transformer-agent), a Java agent to manipulate the time returned by `System.currentTimeMillis()` and `System.nanoTime()`.

# Example application: AuthenticationWebServer

The example application is a little authentication webservice where you have to login before you are greeted by the application. The only implemented [OWASP recommendation](https://www.owasp.org/index.php/Authentication_Cheat_Sheet#Prevent_Brute-Force_Attacks) is a user lockout system to prevent brute force attacks on the password.

*Disclaimer: this application is only intended to give a use case for time-dependent code. It is explicitly __not__ an example of how to do authentication in a web application!*

# Running the application server manually
* Build the application server:
  ```
  mvn clean package
  ```
* Start the application server.
  ```
  java -jar target/time-transformer-examples-1.0.0-SNAPSHOT.jar
  ```
* Browse to `http://localhost:8080/login`
* Login using username `admin` and password `admin`

## Configuring the application:
* `-DwebserverPort=[PORT]` runs the webserver on port `[PORT]`. Defaults to 8080.
* `-DtestingMode=true` enables the `http://localhost:8080/test/transformtime` endpoint in the webserver. Requires the time-transformer-agent to be attached to the JVM as a `javaagent`:
    ```
    java -javaagent:/path/to/time-transformer-agent-1.0.0.jar -DtestingMode=true -jar target/time-transformer-examples-1.0.0-SNAPSHOT.jar
    ```


# End-to-end test scenarios:

*These scenarios are implemented using Selenium's `HtmlUnitDriver` and can be found in [`AuthenticationWebserverE2ECase.java`](https://github.com/TOPdesk/time-transformer-examples/tree/master/src/test/java/com/topdesk/timetransformer/examples/webserver/AuthenticationWebserverE2ECase.java)*

## Correct login:

* When the user browses to `http://localhost:8080/login` and logs in using username `admin` and password `admin`.
* Then the message "Welcome admin" should be shown.

## Incorrect login:
* When the user browses to `http://localhost:8080/login` and logs in using username `admin` and password `wrong`.
* Then the message "Login incorrect" should be shown.

## Can't login with correct credentials when locked out
* Given that the user is locked out by browsing to `http://localhost:8080/login` and logging in using username `admin` and password `wrong` three times.
* When the user logs in using username `admin` and password `admin`.
* Then the message "Login incorrect" should be shown.

## Can login with correct credentials after lockout time passed
* Given that the user is locked out by browsing to `http://localhost:8080/login` and logging in using username `admin` and password `wrong` three times.
* When the user lockout time of five minutes has passed (Or at least the server believes so, using the `TimeTransformer`).
* When the user logs in using username `admin` and password `admin`.
* Then the message "Welcome admin" should be shown.

# Contributing:
By adding your name to the `AUTHORS` file, you accept that your changes will become public under the license specified in the `LICENSE` file.
