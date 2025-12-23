package com.taskflow.taskflow.config;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PASSWORD_HASH = "$2a$10$cOFFrd9N/J0yFfvnDxRz..TjvE1SKeMVBIqKSsE3V9fX1pYZzcJ0e";

    private final EmployeeRepository employeeRepository;

    public DataInitializer(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDatabase() {
        log.info("🚀 Data Initialization started...");

        Employee admin = createAdminIfNotExists();
        Employee manager = createManagerIfNotExists(admin);
        createEmployeesIfNotExist(manager);
        log.info("✅ Data initialization complete.");
    }

    private Employee createAdminIfNotExists() {
        String email = "lionel.messi@example.com";

        return employeeRepository.findByEmail(email)
            .orElseGet(() -> {
                Employee admin = createEmployee(
                    "Lionel",
                    "Messi",
                    email,
                    PASSWORD_HASH,
                    Role.ADMIN,
                    null
                );

                admin = employeeRepository.save(admin);
                employeeRepository.flush();

                admin.setManager(admin);
                admin = employeeRepository.save(admin);

                log.info("✅ Admin created: {} - {}", admin.getEmail(), admin.getRole());
                return admin;
            });
    }

    private Employee createManagerIfNotExists(Employee admin) {
        String email = "cristiano.ronaldo@example.com";

        return employeeRepository.findByEmail(email)
            .orElseGet(() -> {
                Employee manager = createEmployee(
                    "Cristiano",
                    "Ronaldo",
                    email,
                    PASSWORD_HASH,
                    Role.MANAGER,
                    admin
                );

                manager = employeeRepository.save(manager);
                employeeRepository.flush();

                log.info("✅ Manager created: {} - {}", manager.getEmail(), manager.getRole());
                return manager;
            });
    }

    private void createEmployeesIfNotExist(Employee manager) {
        log.info("🔄 Creating employees...");

        String[][] footballPlayers = {
            {"Neymar", "Jr", "neymar.junior@example.com"},
            {"Kylian", "Mbappé", "kylian.mbappe@example.com"},
            {"Erling", "Haaland", "erling.haaland@example.com"},
            {"Kevin", "De Bruyne", "kevin.debruyne@example.com"},
            {"Mohamed", "Salah", "mohamed.salah@example.com"},
            {"Robert", "Lewandowski", "robert.lewandowski@example.com"},
            {"Luka", "Modrić", "luka.modric@example.com"},
            {"Vinícius", "Júnior", "vinicius.junior@example.com"},
            {"Jude", "Bellingham", "jude.bellingham@example.com"},
            {"Bukayo", "Saka", "bukayo.saka@example.com"},
            {"Phil", "Foden", "phil.foden@example.com"},
            {"Bernardo", "Silva", "bernardo.silva@example.com"},
            {"Bruno", "Fernandes", "bruno.fernandes@example.com"},
            {"Harry", "Kane", "harry.kane@example.com"},
            {"Rodri", "Hernández", "rodri.hernandez@example.com"},
            {"Thibaut", "Courtois", "thibaut.courtois@example.com"},
            {"Alisson", "Becker", "alisson.becker@example.com"},
            {"Virgil", "van Dijk", "virgil.vandijk@example.com"},
            {"Rúben", "Dias", "ruben.dias@example.com"},
            {"Trent", "Alexander-Arnold", "trent.alexanderarnold@example.com"},
            {"Pedri", "González", "pedri.gonzalez@example.com"},
            {"Gavi", "Páez", "gavi.paez@example.com"},
            {"Federico", "Valverde", "federico.valverde@example.com"},
            {"Eduardo", "Camavinga", "eduardo.camavinga@example.com"},
            {"Aurélien", "Tchouaméni", "aurelien.tchouameni@example.com"},
            {"Jamal", "Musiala", "jamal.musiala@example.com"},
            {"Florian", "Wirtz", "florian.wirtz@example.com"},
            {"Victor", "Osimhen", "victor.osimhen@example.com"},
            {"Rafael", "Leão", "rafael.leao@example.com"},
            {"Khvicha", "Kvaratskhelia", "khvicha.kvaratskhelia@example.com"},
            {"Martin", "Ødegaard", "martin.odegaard@example.com"}
        };

        int created = 0;
        int existing = 0;

        for (String[] player : footballPlayers) {
            String firstName = player[0];
            String lastName = player[1];
            String email = player[2];


            if (employeeRepository.findByEmail(email).isEmpty()) {
                Employee employee = createEmployee(
                    firstName,
                    lastName,
                    email,
                    PASSWORD_HASH,
                    Role.EMPLOYEE,
                    manager
                );

                employeeRepository.save(employee);
                created++;
                log.info("✅ Employee Created: {} {} - {}", firstName, lastName, email);
            } else {
                existing++;
                log.debug("ℹ️  Employee already exist: {}", email);
            }
        }

        employeeRepository.flush();
        log.info("📊 Summary: {} employees created, {} already saved.  Total: {}",
                 created, existing, footballPlayers.length);
    }
    
    private Employee createEmployee(String firstName, String lastName, String email,
                                   String passwordHash, Role role, Employee manager) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPasswordHash(passwordHash);
        employee.setRole(role);
        employee.setManager(manager);
        return employee;
    }
}

