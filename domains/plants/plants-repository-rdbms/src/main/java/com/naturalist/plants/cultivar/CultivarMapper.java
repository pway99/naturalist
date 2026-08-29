package com.naturalist.plants.cultivar;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code cultivar} table. Reads JOIN {@code plant_species} and project its
 * {@code name} as {@code plant_name} (so the DBO speaks the species slug, never the numeric id);
 * writes name the columns and bind camelCase params, nested-selecting {@code plant_species_id} from
 * the slug. A 0-row {@link #insert} means the referenced species is absent — the adapter maps that
 * to {@code EntityNotFoundException}. {@code id} is a DB-generated identity.
 */
@Mapper
interface CultivarMapper {

    String COLUMNS = """
        c.id, c.name, s.name AS plant_name, c.common_name, c.variety_type, c.fruit_type,
        c.seed_saving_policy, c.seed_source, c.garden_notes,
        c.description_preschool, c.description_elementary, c.description_secondary, c.description_university
        """;

    String FROM = " FROM cultivar c JOIN plant_species s ON s.id = c.plant_species_id ";

    @Select("SELECT " + COLUMNS + FROM + "WHERE c.name = #{name}")
    CultivarDbo selectByName(String name);

    @Select("""
        <script>
        SELECT c.id, c.name, s.name AS plant_name, c.common_name, c.variety_type, c.fruit_type,
               c.seed_saving_policy, c.seed_source, c.garden_notes,
               c.description_preschool, c.description_elementary, c.description_secondary, c.description_university
        FROM cultivar c JOIN plant_species s ON s.id = c.plant_species_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CultivarDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + "ORDER BY c.name LIMIT #{limit} OFFSET #{offset}")
    List<CultivarDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM cultivar ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    /** Cultivars whose species FK resolves to the given species slug — backs getByPlantName. */
    @Select("SELECT " + COLUMNS + FROM + "WHERE s.name = #{plantName} ORDER BY c.name")
    List<CultivarDbo> selectByPlantName(String plantName);

    @Insert("""
        INSERT INTO cultivar (name, plant_species_id, common_name, variety_type, fruit_type,
            seed_saving_policy, seed_source, garden_notes,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, s.id, #{commonName}, #{varietyType}, #{fruitType},
            #{seedSavingPolicy}, #{seedSource}, #{gardenNotes},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM plant_species s WHERE s.name = #{plantName}
        """)
    int insert(CultivarDbo dbo);

    @Update("""
        UPDATE cultivar SET
            plant_species_id = (SELECT id FROM plant_species WHERE name = #{plantName}),
            common_name = #{commonName}, variety_type = #{varietyType}, fruit_type = #{fruitType},
            seed_saving_policy = #{seedSavingPolicy}, seed_source = #{seedSource}, garden_notes = #{gardenNotes},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(CultivarDbo dbo);
}
