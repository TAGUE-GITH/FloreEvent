package com.floreevent.catalog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.catalog.CatalogDtos.CategoryRequest;
import com.floreevent.catalog.CatalogDtos.CategoryResponse;
import com.floreevent.user.Role;

import jakarta.validation.Valid;

/** Categories du catalogue : consultees par toute l'equipe, modifiees par le gerant. */
@RestController
@RequestMapping("/api/categories")
@PreAuthorize(Role.STAFF)
@Transactional
public class CategoryController {

	private final CategoryRepository categories;
	private final CatalogService catalog;

	public CategoryController(CategoryRepository categories, CatalogService catalog) {
		this.categories = categories;
		this.catalog = catalog;
	}

	private CategoryResponse response(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), catalog.itemCount(category.getId()));
	}

	@GetMapping
	public List<CategoryResponse> list() {
		return categories.findAllByOrderByNameAsc().stream().map(this::response).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('GERANT')")
	public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
		return response(catalog.createCategory(request.name()));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('GERANT')")
	public CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return response(catalog.renameCategory(id, request.name()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('GERANT')")
	public void delete(@PathVariable Long id) {
		catalog.deleteCategory(id);
	}
}
