from extract_geo_attrs import parse_help_content


def test_parse_help_content():
    content = """
    `pscale`:
    #type: float

    `Cd`:
    #type: vec3

    `invalid_no_type`:
    Some description without type

    `orient`:
    #type: vector4
    """
    data = {
        "float": [],
        "vector": [],
        "vector4": [],
        "int": [],
        "string": [],
        "dict": [],
    }

    parse_help_content(content, data)

    assert "pscale" in data["float"]
    assert "Cd" in data["vector"]
    assert "orient" in data["vector4"]
    assert "invalid_no_type" not in data["float"]
    assert "invalid_no_type" not in data["string"]
