package com.facebook.presto.metadata;

import com.facebook.presto.tuple.TupleInfo;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.facebook.presto.metadata.MetadataUtil.ColumnMetadataListBuilder.columnsBuilder;
import static com.facebook.presto.metadata.MetadataUtil.getType;
import static com.facebook.presto.tuple.TupleInfo.Type.FIXED_INT_64;
import static com.facebook.presto.tuple.TupleInfo.Type.VARIABLE_BINARY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.transform;

public class InformationSchemaMetadata
        extends AbstractInformationSchemaMetadata
{
    public static final String INFORMATION_SCHEMA = "information_schema";

    public static final String TABLE_COLUMNS = "columns";
    public static final String TABLE_TABLES = "tables";
    public static final String TABLE_INTERNAL_FUNCTIONS = "__internal_functions__";
    public static final String TABLE_INTERNAL_PARTITIONS = "__internal_partitions__";

    private static final Map<String, List<ColumnMetadata>> METADATA = ImmutableMap.<String, List<ColumnMetadata>>builder()
            .put(TABLE_COLUMNS, columnsBuilder()
                    .column("table_catalog", VARIABLE_BINARY)
                    .column("table_schema", VARIABLE_BINARY)
                    .column("table_name", VARIABLE_BINARY)
                    .column("column_name", VARIABLE_BINARY)
                    .column("ordinal_position", FIXED_INT_64)
                    .column("column_default", VARIABLE_BINARY)
                    .column("is_nullable", VARIABLE_BINARY)
                    .column("data_type", VARIABLE_BINARY)
                    .build())
            .put(TABLE_TABLES, columnsBuilder()
                    .column("table_catalog", VARIABLE_BINARY)
                    .column("table_schema", VARIABLE_BINARY)
                    .column("table_name", VARIABLE_BINARY)
                    .column("table_type", VARIABLE_BINARY)
                    .build())
            .put(TABLE_INTERNAL_FUNCTIONS, columnsBuilder()
                    .column("function_name", VARIABLE_BINARY)
                    .column("argument_types", VARIABLE_BINARY)
                    .column("return_type", VARIABLE_BINARY)
                    .build())
            .put(TABLE_INTERNAL_PARTITIONS, columnsBuilder()
                    .column("table_catalog", VARIABLE_BINARY)
                    .column("table_schema", VARIABLE_BINARY)
                    .column("table_name", VARIABLE_BINARY)
                    .column("partition_number", FIXED_INT_64)
                    .column("partition_key", VARIABLE_BINARY)
                    .column("partition_value", VARIABLE_BINARY)
                    .build())
            .build();

    @Inject
    public InformationSchemaMetadata()
    {
        super(INFORMATION_SCHEMA, METADATA);
    }

    static TupleInfo informationSchemaTupleInfo(String tableName)
    {
        checkArgument(METADATA.containsKey(tableName), "table does not exist: %s", tableName);
        return new TupleInfo(transform(METADATA.get(tableName), getType()));
    }

    static int informationSchemaColumnIndex(String tableName, String columnName)
    {
        checkArgument(METADATA.containsKey(tableName), "table does not exist: %s", tableName);
        List<ColumnMetadata> columns = METADATA.get(tableName);
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(columnName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("column does not exist: " + columnName);
    }
}