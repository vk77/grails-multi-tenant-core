package grails.plugin.multitenant.core.groovy.compiler;

import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.PropertyNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import java.lang.reflect.Modifier;

import grails.plugin.multitenant.TenantId;

/**
 * Performs an ast transformation on a class - adds a tenantId property to the subject class.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION	)
public class SharedTenantASTTransformation implements ASTTransformation {
// ========================================================================================================================
//    Static Fields
// ========================================================================================================================

    private static final Log LOG = LogFactory.getLog(TenantASTTransformation.class);
	private static final String key = "tenants";

// ========================================================================================================================
//    Public Instance Methods
// ========================================================================================================================

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        for (ASTNode astNode : astNodes) {
            if (astNode instanceof ClassNode) {
                ClassNode classNode = (ClassNode) astNode;
				final boolean hasTenants = GrailsASTUtils.hasProperty(classNode, key);
				final boolean hasHasMany = GrailsASTUtils.hasProperty(classNode, "hasMany");
				if(!hasTenants){
					if(!hasHasMany){
						MapExpression me = new MapExpression();
						me.addMapEntryExpression(new ConstantExpression(key), new ClassExpression(new ClassNode(TenantId.class)));
						classNode.addProperty("hasMany", Modifier.PUBLIC | Modifier.STATIC, new ClassNode(java.util.Map.class), me, null, null);
					} else {
						PropertyNode hm = classNode.getProperty("hasMany");
						MapExpression me = (MapExpression)hm.getInitialExpression();
						me.addMapEntryExpression(new ConstantExpression(key), new ClassExpression(new ClassNode(TenantId.class)));
					}
					classNode.addProperty(new PropertyNode(key, Modifier.PUBLIC, new ClassNode(Set.class), classNode, null, null, null));
				}
            }
        }
    }
}
