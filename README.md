# Distributed Supply Management System (DSMS)

Java RMI implementation – Distributed Supply Management System. Three store servers (QC, ON, BC) expose manager and customer operations such as adding items, purchasing across stores, waitlist processing and returns with 30-day policy enforcement.

## Project Structure

```
├── docs/            # Design documentation
├── logs/            # Runtime logs (created automatically)
├── src/
│   ├── main/
│   │   ├── java/com/concordia/dsms/common   # RMI contracts and shared DTOs
│   │   ├── java/com/concordia/dsms/server   # Server implementation and launcher
│   │   └── java/com/concordia/dsms/client   # CLI clients for managers and customers
│   └── test/java                             # JUnit tests
└── pom.xml
```

## Prerequisites
- Java 17+
- Apache Maven 3.8+

## Building
```
mvn clean package
```
The command compiles the project, runs unit tests and produces `target/dsms-1.0-SNAPSHOT.jar` with the server launcher as the entry point.

## Running the Servers
```
java -jar target/dsms-1.0-SNAPSHOT.jar
```
This starts all three store servers (QC, ON, BC) and loads initial inventory. Logs are written to the `logs/` folder.

## Running Clients
In different terminals after the servers are running:

### Manager Client
```
java -cp target/dsms-1.0-SNAPSHOT.jar com.concordia.dsms.client.ManagerClient <managerId>
```
Example: `java -cp target/dsms-1.0-SNAPSHOT.jar com.concordia.dsms.client.ManagerClient QCM0001`

### Customer Client
```
java -cp target/dsms-1.0-SNAPSHOT.jar com.concordia.dsms.client.CustomerClient <customerId>
```
Example: `java -cp target/dsms-1.0-SNAPSHOT.jar com.concordia.dsms.client.CustomerClient QCU1000`

Each client interaction is persisted in `logs/clients/<ID>.log`.

## Testing
```
mvn test
```
Runs the automated JUnit tests validating core inventory, purchase, waitlist and return flows.

## Notes
- Customer budget is set to CAD 1000 by default and enforced across stores. Customers can purchase unlimited items from their home store and at most one item from each remote store.
- Waitlisted customers are automatically served when stock becomes available.
- Returns are only accepted within 30 days of the purchase date and only by the customer who purchased the item.
