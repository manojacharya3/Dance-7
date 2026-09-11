package com.studioos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RoleScopeFilter extends OncePerRequestFilter {
    private final ScopeService scope;
    public RoleScopeFilter(ScopeService scope) { this.scope = scope; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/") && !path.startsWith("/api/auth") && scope.developer()) {
            String method = request.getMethod();
            boolean readOnly = "GET".equalsIgnoreCase(method)
                && (path.equals("/api/admin/users")
                    || path.startsWith("/api/ai/admin")
                    || path.matches("/api/(students|instructors|batches|memberships|payments|invoices|branches|feedback)(/\\d+)?")
                    || path.matches("/api/student-batches/(batch|student)/\\d+")
                    || path.startsWith("/api/attendance"));
            boolean feedbackTriage = ("POST".equalsIgnoreCase(method) && path.equals("/api/feedback"))
                || ("PATCH".equalsIgnoreCase(method) && path.matches("/api/feedback/\\d+/(status|notes)"));
            if (!readOnly && !feedbackTriage) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Developer access is read-only, except feedback triage."); return; }
        }
        if (path.startsWith("/api/") && !path.startsWith("/api/auth") && scope.instructor()) {
            if (path.startsWith("/api/invoices") || path.startsWith("/api/admin")) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Instructor access is limited to assigned students, batches, attendance, instructors, memberships, and payments."); return; }
            if (path.startsWith("/api/memberships") || path.startsWith("/api/payments")) {
                boolean scopedRead = "GET".equalsIgnoreCase(request.getMethod())
                    && (path.equals("/api/memberships") || path.equals("/api/payments") || path.matches("/api/(memberships|payments)/\\d+"));
                if (!scopedRead) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Instructors have read-only access to assigned memberships and payments."); return; }
            }
        }
        String requestedBranch = request.getParameter("branchId");
        if (requestedBranch != null && (scope.branchHead() || scope.instructor()) && !scope.owner() && !requestedBranch.equals(String.valueOf(scope.branchId()))) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied for this branch"); return; }
        chain.doFilter(request, response);
    }
}
