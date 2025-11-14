package fr.anisekai.core.persistence.interfaces;

import fr.anisekai.core.persistence.domain.Entity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.io.Serializable;

@NoRepositoryBean
public interface AnisekaiRepository<T extends Entity<ID>, ID extends Serializable> extends
        ListCrudRepository<T, ID>,
        ListPagingAndSortingRepository<T, ID>,
        QueryByExampleExecutor<T>,
        CloseableContext {

}
