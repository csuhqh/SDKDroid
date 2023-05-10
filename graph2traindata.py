import json

def save_file(version:int, filename, num, data: list):
    with open(f"./outputs/android-{version}/{filename}", "w") as f:
        f.write(str(num) + "\n")
        for i, da in enumerate(data):
            if i == len(data) - 1:
                f.write(da)
            else:
                f.write(da + "\n")


def read_graph(version:int):
    with open(f"./outputs/android-{version}/entities.json", "r") as f:
        entities = json.loads(f.read())
    with open(f"./outputs/android-{version}/relations.json", "r") as f:
        relations = json.loads(f.read())
    return entities, relations

def main():
    # 加载配置
    with open("config.json", "r") as f:
        config = json.loads(f.read())
    version = config['android-version']
    entities, relations = read_graph(version)

    entity_id_mapping = {}
    data = []
    i = 0
    for key, value in entities.items():
        for v in value:
            data.append(v + "\t" + str(i))
            entity_id_mapping[v] = str(i)  #每个实体变成一个id
            i += 1
    save_file(version, "entity2id.txt", i, data)

    relation_id_mapping = {}
    data = []
    i = 0
    for key in relations.keys():
        data.append(key + "\t" + str(i))
        relation_id_mapping[key] = str(i)
        i += 1
    save_file(version, "relation2id.txt", i, data)

    data = []
    i = 0
    for key, value in relations.items():
        for va in value:
            data.append(entity_id_mapping[va[0]] + "\t" + entity_id_mapping[va[1]] + "\t" + relation_id_mapping[key])
            i += 1
    save_file(version, "train2id.txt", i, data)

if __name__ == '__main__':
    main()