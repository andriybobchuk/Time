package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.Category
import com.andriybobchuk.time.mooney.domain.CoreRepository

class GetCategoriesUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): List<Category> {
        return repository.getAllCategories()
    }
    
    operator fun invoke(id: String): Category? {
        return repository.getCategoryById(id)
    }
    
    fun getTopLevelCategories(): List<Category> {
        return repository.getTopLevelCategories()
    }
    
    fun getSubcategories(parentId: String): List<Category> {
        return repository.getSubcategories(parentId)
    }
} 