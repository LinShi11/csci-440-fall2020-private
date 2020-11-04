package edu.montana.csci.csci440.helpers;

import edu.montana.csci.csci440.model.Album;
import edu.montana.csci.csci440.model.Employee;
import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class EmployeeHelper {
    public static String makeEmployeeTree() {
        // TODO, change this to use a single query operation to get all employees
        List<Employee> employees = Employee.all(); // root employee

        Map<Long, List<Employee>> employeeMap = new HashMap<>();
        int columns = employees.size();
        int i = 0;
        while(i < columns){
            List<Employee> empList= new ArrayList<Employee>() ;
            for(int j = 0; j < columns; j++){
                if(employees.get(j).getReportsTo() == employees.get(i).getEmployeeId()){
                    empList.add(employees.get(j));
                }
            }
            employeeMap.put(employees.get(i).getEmployeeId(), empList);
            i+=1;
        }

        // and use this data structure to maintain reference information needed to build the tree structure
        Employee employee = employees.get(0);
        return "<ul>" + makeTree(employee, employeeMap) + "</ul>";
    }

    // TODO - currently this method just usese the employee.getReports() function, which
    //  issues a query.  Change that to use the employeeMap variable instead
    public static String makeTree(Employee employee, Map<Long, List<Employee>> employeeMap) {
        String list = "<li><a href='/employees" + employee.getEmployeeId() + "'>"
                + employee.getEmail() + "</a><ul>";
        List<Employee> reports = employeeMap.get(employee.getEmployeeId());
        for (Employee report : reports) {
            list += makeTree(report, employeeMap);
        }
        return list + "</ul></li>";
    }
}
