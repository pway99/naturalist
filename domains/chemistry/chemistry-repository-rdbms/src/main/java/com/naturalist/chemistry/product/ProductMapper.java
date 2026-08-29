package com.naturalist.chemistry.product;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code product} aggregate. The parent row is {@code name} +
 * {@code display_name}; the {@code Set<CompoundName>} formulation and the properties map live in
 * child tables, loaded one batched query each (keyed on product name). Child inserts nested-select
 * the {@code product_id}/{@code compound_id} from names, so no generated id is threaded back.
 */
@Mapper
interface ProductMapper {

    @Select("SELECT id, name, display_name FROM product WHERE name = #{name}")
    ProductDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, display_name FROM product
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<ProductDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT id, name, display_name FROM product ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<ProductDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM product ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (batched; project product.name for grouping, compound.name for the value) ──
    @Select("""
        <script>
        SELECT p.name AS product_name, c.name AS compound_name
        FROM product_compound pc
        JOIN product p ON p.id = pc.product_id
        JOIN compound c ON c.id = pc.compound_id
        WHERE p.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<ProductCompoundDbo> selectProductCompounds(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT p.name AS product_name, pp.property_key, pp.property_value
        FROM product_property pp JOIN product p ON p.id = pp.product_id
        WHERE p.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<ProductPropertyDbo> selectProductProperties(@Param("names") Collection<String> names);

    /** Product names whose formulation includes the given compound — backs getByCompoundName. */
    @Select("""
        SELECT p.name FROM product p
        JOIN product_compound pc ON pc.product_id = p.id
        JOIN compound c ON c.id = pc.compound_id
        WHERE c.name = #{compoundName} ORDER BY p.name
        """)
    List<String> selectProductNamesByCompoundName(String compoundName);

    // ── writes ───────────────────────────────────────────────────────────────
    @Insert("INSERT INTO product (name, display_name) VALUES (#{name}, #{displayName})")
    void insertProduct(ProductDbo dbo);

    @Update("UPDATE product SET display_name = #{displayName} WHERE name = #{name}")
    int updateProduct(ProductDbo dbo);

    @Insert("""
        INSERT INTO product_compound (product_id, compound_id)
        SELECT p.id, c.id FROM product p, compound c
        WHERE p.name = #{productName} AND c.name = #{compoundName}
        """)
    int insertProductCompound(ProductCompoundDbo dbo);

    @Insert("""
        INSERT INTO product_property (product_id, property_key, property_value)
        SELECT id, #{propertyKey}, #{propertyValue} FROM product WHERE name = #{productName}
        """)
    int insertProductProperty(ProductPropertyDbo dbo);

    @Delete("DELETE FROM product_compound WHERE product_id = (SELECT id FROM product WHERE name = #{name})")
    void deleteProductCompounds(String name);

    @Delete("DELETE FROM product_property WHERE product_id = (SELECT id FROM product WHERE name = #{name})")
    void deleteProductProperties(String name);
}
