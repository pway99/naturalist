package com.naturalist.console.admin;

import com.naturalist.infrastructure.DomainService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Renders {@code /admin/domain-services} — the discovered-bean diagnostic
 * surface described in {@code docs/plans/admin-console.md}.
 *
 * <p>The kernel-level abstraction for this view is the
 * {@link DomainService} marker; the registry is owned by
 * {@code adapters/spring-runtime/} via {@code DomainServiceScan}, which
 * imports the marker and registers each annotated class as a bean. The
 * console is a Spring Boot composition root and reads its own container
 * here — Spring's {@link ApplicationContext} is the natural pull-source,
 * so this controller does not import scan internals or any domain
 * {@code *-core} package.
 */
@Controller
public class AdminDomainServicesController {

    private final ApplicationContext applicationContext;

    AdminDomainServicesController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/admin/domain-services")
    String domainServices(Model model) {
        var domains = applicationContext.getBeansWithAnnotation(DomainService.class)
                .values().stream()
                .map(bean -> Bean.from(bean.getClass()))
                .collect(Collectors.groupingBy(
                        Bean::domain,
                        TreeMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new Domain(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(Bean::fullyQualifiedName))
                                .toList()))
                .toList();
        model.addAttribute("domains", domains);
        return "admin/domain-services";
    }

    public record Domain(String name, List<Bean> beans) {
    }

    public record Bean(String domain, String simpleName, String fullyQualifiedName) {

        static Bean from(Class<?> type) {
            return new Bean(domainOf(type), type.getSimpleName(), type.getName());
        }

        private static String domainOf(Class<?> type) {
            String packageName = type.getPackageName();
            String prefix = "com.naturalist.";
            if (!packageName.startsWith(prefix)) {
                return packageName;
            }
            String tail = packageName.substring(prefix.length());
            int dot = tail.indexOf('.');
            return dot < 0 ? tail : tail.substring(0, dot);
        }
    }
}
