package com.example.latencychecker

import com.example.latencychecker.data.local.UsageSnapDao

class UsageRepo(private val dao: UsageSnapDao) {

    suspend fun getAppDataUsage() = dao.getAppDataUsage()
}
