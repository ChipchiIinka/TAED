package edu.penzgtu.taed;

import java.util.List;

public class SearchResult {
    private final List<StudentData> students;

    public SearchResult(List<StudentData> students) {
        this.students = students;
    }

    public List<StudentData> getStudents() {
        return students;
    }

    public static class StudentData {
        private final long studentId;
        private final String studentName;
        private final String pageName;

        public StudentData(long studentId, String studentName, String pageName) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.pageName = pageName;
        }

        public long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getPageName() {
            return pageName;
        }
    }
}