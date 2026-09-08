package com.example.recipecatelog.Repositories;

import com.example.recipecatelog.Model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(Collection<String> names);

    /** Autocomplete: tags whose name starts with the given prefix. */
    Page<Tag> findByNameStartingWithIgnoreCase(String prefix, Pageable pageable);

    Page<Tag> findByNamespaceIgnoreCase(String namespace, Pageable pageable);
}
