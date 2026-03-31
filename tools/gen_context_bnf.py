import hou


def generate_context_bnf():
    contexts = []

    for ctx in hou.vexContexts():
        name = ctx.name().lower()

        if name == "displacement":
            name = "displace"

        contexts.append(name)

    contexts = sorted(list(set(contexts)))

    bnf_rule = "CONTEXT_TYPE ::= " + " | ".join(f"'{c}'" for c in contexts)

    print(bnf_rule)


if __name__ == "__main__":
    generate_context_bnf()
