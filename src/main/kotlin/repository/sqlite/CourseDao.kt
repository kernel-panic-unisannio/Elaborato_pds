package repository.sqlite

import model.Course
import repository.Dao
import java.sql.PreparedStatement
import java.sql.SQLException

class CourseDao(val daoFactory: DaoFactory) : Dao<Course, Pair<String, Int>> {
    override fun create(obj: Course): Boolean {
        val sql = "insert into course values (?, ?)"
        val prepStat = Connection.getConnection().prepareStatement(sql)
        prepStat.setString(1, obj.name)
        prepStat.setInt(2, obj.year)
        try {
            prepStat.execute()
        } catch (e: SQLException) {
            e.stackTrace
            return false
        }
        return true
    }

    override fun read(id: Pair<String, Int>): Course? {
        val sql = "select * from course where name == ? and year == ? limit 1"
        val prepStat = Connection.getConnection().prepareStatement(sql)
        prepStat.setString(1, id.first)
        prepStat.setInt(2, id.second)

        val courseResult = prepStat.executeQuery()

        if (!courseResult.next())
            return null
        val courseName = courseResult.getString(1)
        val courseYear = courseResult.getInt(2)

        val course = Course(courseName, courseYear)

        val userDao = daoFactory.userDao

        userDao.filterBy(prepareUserStatament(courseName, courseYear))
            .map { userDao.read(it) }
            .forEach {
                if (it != null)
                    course.addTeacher(it)
            }

        val groupDao = daoFactory.groupDao
        groupDao.filterBy(prepareGroupStatament(courseName, courseYear))
            .map { groupDao.read(it) }
            .forEach {
                if (it != null) {
                    course.addGroup(it)
                    it.getMembers().forEach { course.addStudent(it) }
                }
            }

        val studentDao = daoFactory.studentDao
        studentDao.filterBy(prepareStudentStatament(courseName, courseYear))
            .map { studentDao.read(it) }
            .forEach {
                if (it != null)
                    course.addStudent(it)
            }

        return course
    }

    override fun update(obj: Course, oldID: Pair<String, Int>): Boolean {
        val sql = "update course set name = ?, year = ? where name = ? and year = ?"
        val prepStat = Connection.getConnection().prepareStatement(sql)
        prepStat.setString(1, obj.name)
        prepStat.setInt(2, obj.year)
        prepStat.setString(3, oldID.first)
        prepStat.setInt(4, oldID.second)

        try {
            prepStat.execute()
        } catch (e: SQLException) {
            e.stackTrace
            return false
        }
        return true
    }

    override fun delete(id: Pair<String, Int>): Boolean {
        val sql = "delete from course where name = ? and year = ?"
        val prepStat = Connection.getConnection().prepareStatement(sql)
        prepStat.setString(1, id.first)
        prepStat.setInt(2, id.second)

        try {
            prepStat.execute()
        } catch (e: SQLException) {
            e.stackTrace
            return false
        }
        return true
    }

    override fun filterBy(stm: PreparedStatement): List<Pair<String, Int>> {
        val result = stm.executeQuery()
        val ids = ArrayList<Pair<String, Int>>()

        while (result.next())
            ids.add(Pair(result.getString(1), result.getInt(2)))

        return ids
    }

    private fun prepareUserStatament(courseName: String, courseYear: Int): PreparedStatement {
        val sql = "select id from user where course_name = ? and course_year = ?"
        val stm = Connection.getConnection().prepareStatement(sql)
        stm.setString(1, courseName)
        stm.setInt(2, courseYear)
        return stm
    }

    private fun prepareGroupStatament(courseName: String, courseYear: Int): PreparedStatement {
        val sql = "select name from \"group\" where course_name = ? and course_year = ?"
        val stm = Connection.getConnection().prepareStatement(sql)
        stm.setString(1, courseName)
        stm.setInt(2, courseYear)
        return stm
    }

    private fun prepareStudentStatament(courseName: String, courseYear: Int): PreparedStatement {
        val sql = "select id from student where \"group\" isnull and course_name = ? and course_year = ?"
        val stm = Connection.getConnection().prepareStatement(sql)
        stm.setString(1, courseName)
        stm.setInt(2, courseYear)
        return stm
    }
}