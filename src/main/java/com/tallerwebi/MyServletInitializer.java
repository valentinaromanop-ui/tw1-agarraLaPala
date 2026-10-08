package com.tallerwebi;

import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.tallerwebi.config.SpringWebConfig;
import jakarta.servlet.*;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  // services and data sources
  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    // El contenedor (Jetty) parsea los formularios con este encoding antes que cualquier filtro
    servletContext.setRequestCharacterEncoding("UTF-8");
    servletContext.setResponseCharacterEncoding("UTF-8");
    super.onStartup(servletContext);
  }

  // services and data sources
  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class<?>[0];
  }

  // controller, view resolver, handler mapping
  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class<?>[] {
      SpringWebConfig.class,
      HibernateConfig.class,
      DatabaseInitializationConfig.class,
    };
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }

  @Override
  protected Filter[] getServletFilters() {
    CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
    encodingFilter.setEncoding("UTF-8");
    encodingFilter.setForceEncoding(true);
    return new Filter[] { encodingFilter };
  }

  @Override
  protected void customizeRegistration(ServletRegistration.Dynamic registration) {
    registration.setMultipartConfig(
      new MultipartConfigElement(System.getProperty("java.io.tmpdir"), 20971520, 41943040, 20971520)
    );
  }
}
